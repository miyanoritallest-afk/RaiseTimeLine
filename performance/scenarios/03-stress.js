/**
 * 03-stress: ストレステスト（限界点探索）
 *
 * 目的: VU 数を段階的に増やして、システムが「壊れ始める」ポイントを特定する。
 *       どの VU 数で p95 が 2000ms を超えるか、エラー率が 5% を超えるかを記録する。
 *
 * 実行方法:
 *   k6 run performance/scenarios/03-stress.js
 *
 * 所要時間: SLO 違反で自動中断されるまで（最大 16 分）
 * 前提条件: seed.js でテストデータが投入済みであること
 *
 * 結果の読み方:
 *   - テストが途中で ABORTED された場合 → その時点の VU 数が限界点
 *   - コンソールの「vu_max」メトリクスで到達した最大 VU 数を確認
 *   - HikariCP（デフォルト接続プール 10本）は 30〜40 VU 付近で枯渇する見込み
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { login, authHeaders } from '../helpers/auth.js';
import { BASE_URL, TEST_USERS } from '../k6.config.js';

export const options = {
  stages: [
    { duration: '2m', target: 20  },
    { duration: '2m', target: 40  },
    { duration: '2m', target: 60  },
    { duration: '2m', target: 80  },
    { duration: '2m', target: 100 },
    { duration: '2m', target: 120 },
    { duration: '2m', target: 150 },
    { duration: '2m', target: 200 },
  ],
  thresholds: {
    // p95 が 2000ms を超えたら即中断 → その VU 数が限界点
    http_req_duration: [{ threshold: 'p(95)<2000', abortOnFail: true, delayAbortEval: '30s' }],
    // エラー率 5% 超でも中断
    http_req_failed: [{ threshold: 'rate<0.05', abortOnFail: true, delayAbortEval: '30s' }],
  },
};

export function setup() {
  // ストレステストでは最大 200 VU が必要だが、TEST_USERS は 50人
  // ラウンドロビンでトークンを割り当てる
  const tokens = [];
  for (const user of TEST_USERS) {
    const token = login(user.email, user.password);
    if (token) tokens.push(token);
    sleep(0.05);
  }
  console.log(`ログイン完了: ${tokens.length} トークン（200 VU まではラウンドロビン）`);
  return { tokens };
}

export default function (data) {
  const token = data.tokens[(__VU - 1) % data.tokens.length];

  // ストレステストはタイムラインに集中（最も重い・ボトルネック筆頭）
  const res = http.get(
    `${BASE_URL}/api/posts?feed=all`,
    authHeaders(token, { endpoint: 'timeline' })
  );
  check(res, { 'タイムライン: 200': (r) => r.status === 200 });

  // think time を短くして最大負荷を模倣
  sleep(0.5);
}
