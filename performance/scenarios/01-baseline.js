/**
 * 01-baseline: ベースライン計測
 *
 * 目的: シングルユーザーで各エンドポイントの素の応答時間を計測する。
 *       並行性ノイズがないため「APIの純粋な処理速度」が分かる。
 *
 * 実行方法:
 *   k6 run performance/scenarios/01-baseline.js
 *
 * 所要時間: 約 3 分
 * 前提条件: seed.js でテストデータが投入済みであること
 */
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { login, authHeaders } from '../helpers/auth.js';
import { randomSeedPostId, randomSeedUserId } from '../helpers/data.js';
import { BASE_URL, TEST_USERS } from '../k6.config.js';

// エンドポイント別の応答時間を個別に記録するカスタムメトリクス
// コンソール出力に timeline_latency{p95} などで表示される
const timelineLatency = new Trend('timeline_latency', true);
const singlePostLatency = new Trend('single_post_latency', true);
const userProfileLatency = new Trend('user_profile_latency', true);
const followersLatency = new Trend('followers_latency', true);

export const options = {
  vus: 1,
  duration: '3m',
  thresholds: {
    // SLO: BCrypt の処理時間込みでログインは 800ms 以内
    'http_req_duration{endpoint:login}': ['p(95)<800'],
    // SLO: 7本の SQL が走るタイムラインは 400ms 以内
    'timeline_latency': ['p(95)<400'],
    'single_post_latency': ['p(95)<150'],
    'user_profile_latency': ['p(95)<150'],
    'followers_latency': ['p(95)<200'],
    // ベースラインでエラーは 0% であるべき
    http_req_failed: ['rate<0.001'],
  },
};

// setup() はテスト全体で 1回だけ実行される
export function setup() {
  const user = TEST_USERS[0];
  const token = login(user.email, user.password);
  return { token };
}

export default function (data) {
  const { token } = data;

  group('タイムライン取得', () => {
    // ページ 1（カーソルなし）
    const res1 = http.get(
      `${BASE_URL}/api/posts?feed=all`,
      authHeaders(token, { endpoint: 'timeline' })
    );
    check(res1, { 'タイムライン p1: 200': (r) => r.status === 200 });
    if (res1.status === 200) {
      timelineLatency.add(res1.timings.duration);

      // ページ 2（カーソルあり）→ カーソルページネーションのパスを確認
      const body = JSON.parse(res1.body);
      const cursor = body.nextCursor;
      if (cursor) {
        sleep(0.3);
        const res2 = http.get(
          `${BASE_URL}/api/posts?feed=all&cursor=${cursor}`,
          authHeaders(token, { endpoint: 'timeline' })
        );
        check(res2, { 'タイムライン p2: 200': (r) => r.status === 200 });
        if (res2.status === 200) timelineLatency.add(res2.timings.duration);
      }
    }
  });

  sleep(1);

  group('単一投稿取得', () => {
    const postId = randomSeedPostId();
    const res = http.get(
      `${BASE_URL}/api/posts/${postId}`,
      authHeaders(token, { endpoint: 'single_post' })
    );
    // 404 はシードIDが存在しない場合があるため許容
    check(res, { '単一投稿: 200 or 404': (r) => r.status === 200 || r.status === 404 });
    if (res.status === 200) singlePostLatency.add(res.timings.duration);
  });

  sleep(1);

  group('ユーザープロフィール取得', () => {
    const userId = randomSeedUserId();
    const res = http.get(
      `${BASE_URL}/api/users/${userId}`,
      authHeaders(token, { endpoint: 'user_profile' })
    );
    check(res, { 'ユーザープロフィール: 200 or 404': (r) => r.status === 200 || r.status === 404 });
    if (res.status === 200) userProfileLatency.add(res.timings.duration);
  });

  sleep(1);

  group('フォロワー一覧取得', () => {
    const userId = randomSeedUserId();
    const res = http.get(
      `${BASE_URL}/api/users/${userId}/followers`,
      authHeaders(token, { endpoint: 'followers' })
    );
    check(res, { 'フォロワー一覧: 200 or 404': (r) => r.status === 200 || r.status === 404 });
    if (res.status === 200) followersLatency.add(res.timings.duration);
  });

  sleep(1);
}

// teardown() はテスト終了後に 1回だけ実行される（ベースラインは読み取りのみなので不要）
