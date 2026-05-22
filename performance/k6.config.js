export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const FRONTEND_URL = __ENV.FRONTEND_URL || 'http://localhost:5173';

// テスト用ユーザー（seed.js で事前登録が必要）
// パスワードはすべて共通: Perf1234!
export const TEST_USERS = Array.from({ length: 50 }, (_, i) => ({
  email: `perf_user_${String(i + 1).padStart(2, '0')}@test.example`,
  password: 'Perf1234!',
  username: `perf_user_${String(i + 1).padStart(2, '0')}`,
}));

// 全シナリオ共通の SLO 閾値（各シナリオで上書き可能）
export const DEFAULT_THRESHOLDS = {
  http_req_duration: ['p(95)<1000'],
  http_req_failed: ['rate<0.01'],
};
