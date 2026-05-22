// 投稿内容プール（280文字以内・バリデーション通過済み）
export const POST_CONTENT_POOL = [
  'パフォーマンステスト用の投稿です。このデータは自動削除されます。',
  'Load test post content - checking timeline performance under concurrent users.',
  'Testing concurrent post creation under load conditions. Please ignore.',
  '負荷テスト中の投稿です。タイムラインのパフォーマンス確認用。',
  'k6 performance test in progress. This post will be cleaned up after the test run.',
  'Stress testing the RaiseTimeLine API. Measuring p95 response times.',
  'パフォーマンス計測中 - ベースライン測定用投稿 #perf',
  'Spike test post: simulating sudden traffic increase scenario.',
];

export function randomPostContent() {
  return POST_CONTENT_POOL[Math.floor(Math.random() * POST_CONTENT_POOL.length)];
}

// seed.js 実行後に投入される投稿 ID の範囲（seed.js 実行後に確認して更新する）
// 初期値は seed.js が最初の 200 件として想定
export const SEED_POST_IDS_RANGE = { min: 1, max: 200 };

export function randomSeedPostId() {
  const { min, max } = SEED_POST_IDS_RANGE;
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

// seed.js 実行後に投入される VU あたりのユーザー ID（1〜50）
export const SEED_USER_IDS_RANGE = { min: 1, max: 50 };

export function randomSeedUserId() {
  const { min, max } = SEED_USER_IDS_RANGE;
  return Math.floor(Math.random() * (max - min + 1)) + min;
}
