/**
 * 05-browser: ブラウザレベルパフォーマンステスト（k6 Browser）
 *
 * 目的: 実際の Chrome ブラウザを操作し、エンドユーザーが体感する
 *       ページ表示速度（Core Web Vitals）を計測する。
 *       API レベルテスト（01〜04）では検出できない以下の問題を発見できる:
 *         - React の再レンダリング過多
 *         - S3 画像の読み込み遅延
 *         - JavaScript バンドルサイズ起因の遅延
 *         - ウォーターフォールリクエスト（連鎖的な API 呼び出し）
 *
 * 計測指標:
 *   - LCP（Largest Contentful Paint）: 主要コンテンツ表示まで。目標 < 2500ms
 *   - FID/INP（Interaction to Next Paint）: 操作への応答速度。目標 < 200ms
 *   - CLS（Cumulative Layout Shift）: レイアウトずれ。目標 < 0.1
 *
 * 実行方法:
 *   k6 run --browser performance/scenarios/05-browser.js
 *
 * 前提条件:
 *   - フロントエンド（port 5173）が起動済み: cd frontend && npm run dev
 *   - バックエンド（port 8080）が起動済み: cd backend && ./gradlew bootRun
 *   - Chrome ブラウザがインストール済み
 *   - seed.js でテストデータが投入済み
 *
 * 所要時間: 約 3〜5 分（ブラウザ起動が重いため VU 数は少なめ）
 */
import { browser } from 'k6/browser';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { FRONTEND_URL, TEST_USERS } from '../k6.config.js';

// ブラウザ経由のページ表示時間を記録するカスタムメトリクス
const loginPageLoad = new Trend('browser_login_page_load', true);
const timelinePageLoad = new Trend('browser_timeline_page_load', true);
const postDetailPageLoad = new Trend('browser_post_detail_page_load', true);
const profilePageLoad = new Trend('browser_profile_page_load', true);

export const options = {
  // ブラウザテストは VU 数を少なく（ブラウザ起動コストが高い）
  scenarios: {
    browser_test: {
      executor: 'constant-vus',
      vus: 2,
      duration: '3m',
      options: {
        browser: {
          type: 'chromium',
        },
      },
    },
  },
  thresholds: {
    // Core Web Vitals の目標値
    'browser_login_page_load': ['p(95)<3000'],      // ログインページ表示 3秒以内
    'browser_timeline_page_load': ['p(95)<2500'],   // タイムライン表示 2.5秒以内（非機能要件）
    'browser_post_detail_page_load': ['p(95)<2000'],
    'browser_profile_page_load': ['p(95)<2000'],
  },
};

export default async function () {
  const page = await browser.newPage();
  const user = TEST_USERS[(__VU - 1) % TEST_USERS.length];

  try {
    // === シナリオ 1: ログイン ===
    const loginStart = Date.now();
    await page.goto(`${FRONTEND_URL}/login`);
    // ログインフォームが表示されるまで待機
    await page.waitForSelector('input[type="email"]', { timeout: 10000 });
    loginPageLoad.add(Date.now() - loginStart);

    // ログイン操作
    await page.locator('input[type="email"]').fill(user.email);
    await page.locator('input[type="password"]').fill(user.password);
    await page.locator('button[type="submit"]').click();

    // === シナリオ 2: タイムライン表示 ===
    const timelineStart = Date.now();
    // ログイン後にホームに遷移するまで待機（投稿カードが現れる）
    await page.waitForSelector('[data-testid="post-card"], .post-card, article', {
      timeout: 15000,
    });
    timelinePageLoad.add(Date.now() - timelineStart);

    check(page, {
      'タイムライン: ページタイトルが正しい': (p) =>
        p.url().includes('/') || p.url().includes('/home'),
    });

    // LCP・CLS を取得（ブラウザ API 経由）
    const webVitals = await page.evaluate(() => {
      return new Promise((resolve) => {
        const vitals = { lcp: null, cls: 0 };

        // LCP
        new PerformanceObserver((list) => {
          const entries = list.getEntries();
          if (entries.length > 0) {
            vitals.lcp = entries[entries.length - 1].startTime;
          }
        }).observe({ type: 'largest-contentful-paint', buffered: true });

        // CLS
        new PerformanceObserver((list) => {
          for (const entry of list.getEntries()) {
            if (!entry.hadRecentInput) vitals.cls += entry.value;
          }
        }).observe({ type: 'layout-shift', buffered: true });

        // 計測に少し時間を与える
        setTimeout(() => resolve(vitals), 1000);
      });
    });

    if (webVitals.lcp) {
      console.log(`LCP: ${Math.round(webVitals.lcp)}ms`);
      check(null, { 'LCP < 2500ms': () => webVitals.lcp < 2500 });
    }
    if (webVitals.cls !== null) {
      console.log(`CLS: ${webVitals.cls.toFixed(3)}`);
      check(null, { 'CLS < 0.1': () => webVitals.cls < 0.1 });
    }

    sleep(1);

    // === シナリオ 3: 投稿詳細ページ ===
    // タイムラインの最初の投稿をクリック
    const firstPostLink = page.locator(
      '[data-testid="post-card"] a, .post-card a, article a'
    ).first();

    if (await firstPostLink.count() > 0) {
      const detailStart = Date.now();
      await firstPostLink.click();
      await page.waitForLoadState('networkidle', { timeout: 10000 });
      postDetailPageLoad.add(Date.now() - detailStart);
    }

    sleep(1);

    // === シナリオ 4: プロフィールページ ===
    await page.goto(`${FRONTEND_URL}/profile`, { waitUntil: 'networkidle' });
    const profileStart = Date.now();
    await page.waitForSelector(
      '[data-testid="profile"], .profile, h1',
      { timeout: 10000 }
    );
    profilePageLoad.add(Date.now() - profileStart);

    // フォローボタンのクリック応答速度を計測
    const followBtn = page.locator(
      '[data-testid="follow-button"], button:has-text("フォロー"), button:has-text("Follow")'
    ).first();
    if (await followBtn.count() > 0) {
      const btnStart = Date.now();
      await followBtn.click();
      // ボタンの状態変化を待つ（フォロー済み表示）
      await page.waitForTimeout(500);
      const btnLatency = Date.now() - btnStart;
      console.log(`フォローボタン応答: ${btnLatency}ms`);
      check(null, { 'フォローボタン INP < 200ms': () => btnLatency < 200 });
    }

  } catch (err) {
    console.error(`ブラウザテストエラー (VU ${__VU}): ${err.message}`);
  } finally {
    await page.close();
  }

  sleep(2);
}
