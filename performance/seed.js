/**
 * テストデータ投入スクリプト
 *
 * 実行方法（初回のみ）:
 *   k6 run --vus 1 --iterations 1 performance/seed.js
 *
 * 投入内容:
 *   - テストユーザー 50人 (perf_user_01〜50@test.example / Perf1234!)
 *   - 投稿 200件 (ユーザーあたり 4件)
 *   - フォロー関係 (各ユーザーが次の5人をフォロー)
 *   - いいね (各ユーザーが10件の投稿にいいね)
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, TEST_USERS } from './k6.config.js';

export const options = {
  vus: 1,
  iterations: 1,
};

const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

function register(user) {
  const res = http.post(
    `${BASE_URL}/api/auth/register`,
    JSON.stringify({
      email: user.email,
      password: user.password,
      username: user.username,
      displayName: `PerfUser ${user.username.slice(-2)}`,
    }),
    JSON_HEADERS
  );

  // 409 = すでに登録済み（再実行時は正常）
  if (res.status === 201) return true;
  if (res.status === 409) {
    console.log(`ユーザー既存（スキップ）: ${user.email}`);
    return true;
  }

  console.error(`登録失敗 (${user.email}): ${res.status} ${res.body}`);
  return false;
}

function loginUser(user) {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email: user.email, password: user.password }),
    JSON_HEADERS
  );
  if (res.status !== 200) return null;
  const body = JSON.parse(res.body);
  return { token: `Bearer ${body.accessToken}`, userId: body.user?.id };
}

function authHeaders(token) {
  return { headers: { 'Content-Type': 'application/json', 'Authorization': token } };
}

function createPost(token, content) {
  const res = http.post(
    `${BASE_URL}/api/posts`,
    JSON.stringify({ content, imageUrls: [] }),
    authHeaders(token)
  );
  if (res.status === 201) return JSON.parse(res.body).id;
  console.warn(`投稿作成失敗: ${res.status}`);
  return null;
}

function followUser(token, targetUserId) {
  const res = http.post(
    `${BASE_URL}/api/users/${targetUserId}/follow`,
    null,
    authHeaders(token)
  );
  // 409 = すでにフォロー済み
  return res.status === 200 || res.status === 201 || res.status === 409;
}

function likePost(token, postId) {
  const res = http.post(
    `${BASE_URL}/api/posts/${postId}/likes`,
    null,
    authHeaders(token)
  );
  // 409 = すでにいいね済み
  return res.status === 200 || res.status === 201 || res.status === 409;
}

export default function () {
  console.log('=== シードデータ投入開始 ===');

  // Step 1: 全ユーザー登録
  console.log('Step 1: ユーザー登録 (50人)...');
  for (const user of TEST_USERS) {
    register(user);
    sleep(0.1); // 登録はBCryptが重いため間隔を置く
  }

  // Step 2: ログインしてトークンとユーザーIDを取得
  console.log('Step 2: ログイン & 投稿作成 (ユーザーあたり4件)...');
  const sessions = [];
  const postIds = [];

  const POST_CONTENTS = [
    'シードデータ投稿 #1 - パフォーマンステスト用',
    'シードデータ投稿 #2 - タイムライン確認用',
    'シードデータ投稿 #3 - 負荷テストデータ',
    'シードデータ投稿 #4 - ベースライン計測用',
  ];

  for (const user of TEST_USERS) {
    const session = loginUser(user);
    if (!session) {
      console.error(`ログイン失敗: ${user.email}`);
      continue;
    }
    sessions.push(session);

    for (const content of POST_CONTENTS) {
      const postId = createPost(session.token, content);
      if (postId) postIds.push(postId);
    }
    sleep(0.05);
  }

  console.log(`投稿作成完了: ${postIds.length} 件 (目標 200 件)`);

  // Step 3: フォロー関係を構築（各ユーザーが次の5人をフォロー）
  console.log('Step 3: フォロー関係の構築...');
  for (let i = 0; i < sessions.length; i++) {
    const { token, userId } = sessions[i];
    if (!userId) continue;

    for (let j = 1; j <= 5; j++) {
      const targetIdx = (i + j) % sessions.length;
      const targetUserId = sessions[targetIdx]?.userId;
      if (targetUserId && targetUserId !== userId) {
        followUser(token, targetUserId);
      }
    }
    sleep(0.05);
  }

  // Step 4: いいね（各ユーザーが10件の投稿にいいね）
  console.log('Step 4: いいねデータの投入...');
  const likeTargets = postIds.slice(0, Math.min(100, postIds.length));
  for (let i = 0; i < sessions.length; i++) {
    const { token } = sessions[i];
    const startIdx = (i * 2) % likeTargets.length;
    for (let j = 0; j < 10 && j < likeTargets.length; j++) {
      likePost(token, likeTargets[(startIdx + j) % likeTargets.length]);
    }
    sleep(0.02);
  }

  console.log('=== シードデータ投入完了 ===');
  console.log(`登録ユーザー: ${sessions.length} 人`);
  console.log(`作成投稿: ${postIds.length} 件`);
  console.log('次のステップ: k6 run performance/scenarios/01-baseline.js');
}
