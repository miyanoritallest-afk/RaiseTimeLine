/**
 * 02-load: 負荷テスト
 *
 * 目的: 現実的な同時接続ユーザー数（最大50VU）での性能劣化を確認する。
 *       SNSとして想定されるユーザー行動ミックス（閲覧・いいね・投稿・検索）を再現。
 *
 * 実行方法:
 *   k6 run performance/scenarios/02-load.js
 *
 *   HTML ダッシュボード付き（リアルタイムでグラフを確認できる）:
 *   k6 run --out web-dashboard performance/scenarios/02-load.js
 *   → ブラウザで http://localhost:5665 を開く
 *
 * 所要時間: 約 9 分
 * 前提条件: seed.js でテストデータが投入済みであること
 *
 * teardown() で投稿作成テストが生成したデータを自動削除する。
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { login, authHeaders } from '../helpers/auth.js';
import { randomPostContent, randomSeedPostId } from '../helpers/data.js';
import { BASE_URL, TEST_USERS } from '../k6.config.js';

export const options = {
  stages: [
    { duration: '30s', target: 10 },  // 0→10 VU ランプアップ
    { duration: '2m',  target: 10 },  // 10 VU で保持
    { duration: '30s', target: 30 },  // 10→30 VU ランプアップ
    { duration: '2m',  target: 30 },  // 30 VU で保持
    { duration: '30s', target: 50 },  // 30→50 VU ランプアップ
    { duration: '3m',  target: 50 },  // 50 VU で保持（最高負荷）
    { duration: '30s', target: 0  },  // ランプダウン
  ],
  thresholds: {
    'http_req_duration{endpoint:timeline}': ['p(95)<500'],
    'http_req_duration{endpoint:like}': ['p(95)<300'],
    'http_req_duration{endpoint:create_post}': ['p(95)<400'],
    'http_req_duration{endpoint:search}': ['p(95)<300'],
    // 409（いいね重複）は正常応答のため failure にカウントしない
    // k6 の http_req_failed は status >= 400 かつ check 失敗でカウントされる
    http_req_failed: ['rate<0.01'],
  },
};

// setup(): 全テストユーザー分のトークンを事前取得
// VU ごとにログインすると BCrypt が CPU を枯渇させるため必須
export function setup() {
  const tokens = [];
  for (const user of TEST_USERS) {
    const token = login(user.email, user.password);
    if (token) tokens.push(token);
    sleep(0.05); // ログインリクエストを分散させる
  }
  console.log(`ログイン完了: ${tokens.length} トークン取得`);

  // teardown() でテスト中作成投稿を削除するため、管理者トークンを別途保持
  return { tokens, createdPostIds: [] };
}

export default function (data) {
  // __VU: 1始まりの VU 番号。トークン配列からラウンドロビンで割り当て
  const token = data.tokens[(__VU - 1) % data.tokens.length];
  const headers = authHeaders(token);
  const rand = Math.random();

  if (rand < 0.60) {
    // 60%: タイムライン閲覧（最も頻度が高い操作）
    const res = http.get(
      `${BASE_URL}/api/posts?feed=all`,
      { ...headers, tags: { endpoint: 'timeline' } }
    );
    check(res, { 'タイムライン: 200': (r) => r.status === 200 });

  } else if (rand < 0.80) {
    // 20%: いいね（409 = すでにいいね済み は正常応答）
    const postId = randomSeedPostId();
    const res = http.post(
      `${BASE_URL}/api/posts/${postId}/likes`,
      null,
      { ...headers, tags: { endpoint: 'like' } }
    );
    check(res, {
      'いいね: 200/201 or 409': (r) => r.status === 200 || r.status === 201 || r.status === 409,
    });

  } else if (rand < 0.90) {
    // 10%: 投稿作成（teardown でクリーンアップ）
    const res = http.post(
      `${BASE_URL}/api/posts`,
      JSON.stringify({ content: randomPostContent(), imageUrls: [] }),
      { ...headers, tags: { endpoint: 'create_post' } }
    );
    const ok = check(res, { '投稿作成: 201': (r) => r.status === 201 });

    // 作成した投稿の ID を teardown 用に記録
    if (ok) {
      try {
        const postId = JSON.parse(res.body).id;
        if (postId) data.createdPostIds.push(postId);
      } catch { /* ignore */ }
    }

  } else {
    // 10%: ユーザー検索
    const res = http.get(
      `${BASE_URL}/api/users?q=perf`,
      { ...headers, tags: { endpoint: 'search' } }
    );
    check(res, { '検索: 200': (r) => r.status === 200 });
  }

  // 1〜2秒のランダム think time（実際のユーザー操作を模倣）
  sleep(1 + Math.random());
}

// teardown(): テスト実行中に作成された投稿を削除する
export function teardown(data) {
  if (!data.createdPostIds || data.createdPostIds.length === 0) {
    console.log('teardown: 削除対象の投稿なし');
    return;
  }

  console.log(`teardown: ${data.createdPostIds.length} 件の投稿を削除中...`);
  // 管理ユーザー（最初のテストユーザー）のトークンで削除
  const adminToken = data.tokens[0];
  let deleted = 0;

  for (const postId of data.createdPostIds) {
    const res = http.del(
      `${BASE_URL}/api/posts/${postId}`,
      null,
      authHeaders(adminToken)
    );
    if (res.status === 200 || res.status === 204) deleted++;
    sleep(0.02);
  }

  console.log(`teardown 完了: ${deleted}/${data.createdPostIds.length} 件削除`);
}
