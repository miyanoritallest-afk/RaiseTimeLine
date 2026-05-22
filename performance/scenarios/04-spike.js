/**
 * 04-spike: スパイクテスト
 *
 * 目的: SNS の「バズり」を模倣し、瞬間的な大量アクセスへの耐性と
 *       トラフィック減少後の回復能力を検証する。
 *
 * 実行方法:
 *   k6 run performance/scenarios/04-spike.js
 *
 * 所要時間: 約 4 分
 * 前提条件: seed.js でテストデータが投入済みであること
 *
 * 結果の読み方:
 *   - スパイク中（1分間）のエラー率 < 10% であればOK（若干の 503 は許容）
 *   - 回復フェーズ（最後の 2分間）でエラー率が 0 に戻っていればOK
 *   - --out web-dashboard で時系列グラフを見ると回復の様子が分かりやすい
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { login, authHeaders } from '../helpers/auth.js';
import { BASE_URL, TEST_USERS } from '../k6.config.js';

export const options = {
  stages: [
    { duration: '1m',  target: 5   },  // 通常トラフィック（ベースライン）
    { duration: '0s',  target: 100 },  // 瞬間スパイク（0秒で100 VU に跳ね上げ）
    { duration: '1m',  target: 100 },  // スパイク継続
    { duration: '0s',  target: 5   },  // 即時収束（0秒で5 VU に戻す）
    { duration: '2m',  target: 5   },  // 回復観察フェーズ
  ],
  thresholds: {
    // スパイク中は一定のエラーを許容（サーバーが過負荷になることを想定）
    http_req_duration: ['p(95)<1500'],
    http_req_failed: ['rate<0.10'],
  },
};

export function setup() {
  const tokens = [];
  for (const user of TEST_USERS) {
    const token = login(user.email, user.password);
    if (token) tokens.push(token);
    sleep(0.05);
  }
  console.log(`スパイクテスト準備完了: ${tokens.length} トークン`);
  return { tokens };
}

export default function (data) {
  const token = data.tokens[(__VU - 1) % data.tokens.length];

  const res = http.get(
    `${BASE_URL}/api/posts?feed=all`,
    authHeaders(token, { endpoint: 'timeline' })
  );
  // スパイク中は 503 Service Unavailable も想定内として許容
  check(res, {
    'タイムライン: 200 or 503': (r) => r.status === 200 || r.status === 503,
  });

  sleep(1);
}
