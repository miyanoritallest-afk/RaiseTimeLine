/**
 * シードデータ全削除スクリプト
 *
 * 実行方法（テスト環境をリセットしたいときのみ）:
 *   k6 run --vus 1 --iterations 1 performance/cleanup.js
 *
 * 削除内容:
 *   - perf_user_* が作成した全投稿・コメント
 *   - perf_user_* のフォロー関係
 *
 * 注意: ユーザー削除 API が未実装のため、ユーザーアカウント自体は残る。
 *       アカウントも含めて完全リセットしたい場合は代わりに:
 *         docker compose down -v && docker compose up -d
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, TEST_USERS } from './k6.config.js';

export const options = {
  vus: 1,
  iterations: 1,
};

const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

function loginUser(user) {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email: user.email, password: user.password }),
    JSON_HEADERS
  );
  if (res.status !== 200) {
    console.warn(`ログイン失敗（スキップ）: ${user.email}`);
    return null;
  }
  const body = JSON.parse(res.body);
  return { token: `Bearer ${body.accessToken}`, userId: body.user?.id };
}

function authHeaders(token) {
  return { headers: { 'Content-Type': 'application/json', 'Authorization': token } };
}

function deleteUserPosts(token, userId) {
  let deleted = 0;
  let cursor = null;

  // 全投稿を取得して削除（ページネーション対応）
  while (true) {
    const url = cursor
      ? `${BASE_URL}/api/users/${userId}/posts?cursor=${cursor}`
      : `${BASE_URL}/api/users/${userId}/posts`;

    const res = http.get(url, authHeaders(token));
    if (res.status !== 200) break;

    const body = JSON.parse(res.body);
    const posts = body.posts || body.content || body || [];
    if (!Array.isArray(posts) || posts.length === 0) break;

    for (const post of posts) {
      const delRes = http.del(`${BASE_URL}/api/posts/${post.id}`, null, authHeaders(token));
      if (delRes.status === 200 || delRes.status === 204) {
        deleted++;
      }
    }

    cursor = body.nextCursor;
    if (!cursor) break;
    sleep(0.05);
  }

  return deleted;
}

function deleteUserFollows(token, userId) {
  // フォロー中のユーザー一覧を取得してフォロー解除
  const res = http.get(`${BASE_URL}/api/users/${userId}/following`, authHeaders(token));
  if (res.status !== 200) return 0;

  const body = JSON.parse(res.body);
  const following = body.users || body || [];
  if (!Array.isArray(following)) return 0;

  let unfollowed = 0;
  for (const user of following) {
    const delRes = http.del(
      `${BASE_URL}/api/users/${user.id}/follow`,
      null,
      authHeaders(token)
    );
    if (delRes.status === 200 || delRes.status === 204) unfollowed++;
  }
  return unfollowed;
}

export default function () {
  console.log('=== シードデータ削除開始 ===');

  let totalPosts = 0;
  let totalFollows = 0;
  let processedUsers = 0;

  for (const user of TEST_USERS) {
    const session = loginUser(user);
    if (!session) continue;

    const { token, userId } = session;
    if (!userId) continue;

    const posts = deleteUserPosts(token, userId);
    const follows = deleteUserFollows(token, userId);

    totalPosts += posts;
    totalFollows += follows;
    processedUsers++;

    if (processedUsers % 10 === 0) {
      console.log(`進捗: ${processedUsers}/50 ユーザー処理済み`);
    }

    sleep(0.1);
  }

  console.log('=== 削除完了 ===');
  console.log(`処理ユーザー: ${processedUsers} 人`);
  console.log(`削除投稿: ${totalPosts} 件`);
  console.log(`解除フォロー: ${totalFollows} 件`);
  console.log('※ユーザーアカウント自体は残っています（APIが未実装のため）');
  console.log('※完全リセットは: docker compose down -v && docker compose up -d');
}
