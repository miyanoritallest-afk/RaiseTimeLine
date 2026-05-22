import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from '../k6.config.js';

/**
 * ログインして Bearer トークン文字列を返す。
 * setup() 内で一度だけ呼び出し、全 VU でトークンを使い回すこと。
 * （VU ごとにログインすると BCrypt による CPU 枯渇が起きる）
 */
export function login(email, password) {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email, password }),
    { headers: { 'Content-Type': 'application/json' }, tags: { endpoint: 'login' } }
  );

  check(res, {
    'login: status 200': (r) => r.status === 200,
    'login: accessToken あり': (r) => {
      try {
        return JSON.parse(r.body).accessToken !== undefined;
      } catch {
        return false;
      }
    },
  });

  if (res.status !== 200) {
    console.error(`ログイン失敗 (${email}): ${res.status} ${res.body}`);
    return null;
  }

  return `Bearer ${JSON.parse(res.body).accessToken}`;
}

/** 認証付き JSON リクエストヘッダーを返す */
export function authHeaders(token, extraTags = {}) {
  return {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token,
    },
    tags: extraTags,
  };
}
