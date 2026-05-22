# パフォーマンステスト

RaiseTimeLine の API・ブラウザレベルのパフォーマンスを計測するための k6 テストスイート。

## テスト構成

```
┌─────────────────────────────────────────────────────────┐
│  Layer 2: ブラウザレベル（05-browser.js）                │
│  実際の Chrome を操作してページ表示速度（Core Web Vitals） │
│  を計測。React レンダリング・画像読み込み時間も含む。     │
├─────────────────────────────────────────────────────────┤
│  Layer 1: API レベル（01〜04）                           │
│  バックエンドに直接 HTTP リクエストを送り、              │
│  スループット・同時接続耐性を計測。                      │
└─────────────────────────────────────────────────────────┘
```

| ファイル | 種類 | 目的 | 所要時間 |
|---|---|---|---|
| `01-baseline.js` | API | 1ユーザーで各エンドポイントの素の応答時間を計測 | 約3分 |
| `02-load.js` | API | 最大50同時ユーザーでの負荷テスト | 約9分 |
| `03-stress.js` | API | 限界点（ブレーキングポイント）の特定 | 最大16分 |
| `04-spike.js` | API | バズり想定の瞬間スパイクと回復確認 | 約4分 |
| `05-browser.js` | ブラウザ | Chrome 経由の体感速度・Core Web Vitals | 約3〜5分 |

## パーセンタイルの読み方

```
p50 = 中央値。ユーザーの半数がこれ以下で返ってくる
p95 = 95%のユーザーがこれ以下 ← SLO の判定基準
p99 = ほぼ最悪ケース。外れ値の影響を反映
```

## 事前準備

### 1. k6 インストール（初回のみ）

```powershell
winget install k6
```

インストール確認:
```powershell
k6 version
```

### 2. サーバー起動

```powershell
# PostgreSQL を起動
docker compose up -d

# バックエンド（別ターミナル）
cd backend
./gradlew bootRun

# フロントエンド（05-browser.js 実行時のみ必要。別ターミナル）
cd frontend
npm run dev
```

### 3. テストデータ投入（初回のみ）

```powershell
k6 run --vus 1 --iterations 1 performance/seed.js
```

投入内容:
- テストユーザー 50人（`perf_user_01〜50@test.example` / `Perf1234!`）
- 投稿 200件（ユーザーあたり4件）
- フォロー関係（各ユーザーが5人をフォロー）
- いいね（各投稿に数件）

## テスト実行

### API レベルテスト

```powershell
# ベースライン（まずこれで動作確認）
k6 run performance/scenarios/01-baseline.js

# 負荷テスト
k6 run performance/scenarios/02-load.js

# リアルタイムダッシュボード付き（http://localhost:5665 で確認）
k6 run --out web-dashboard performance/scenarios/02-load.js

# ストレステスト（限界点を探す）
k6 run performance/scenarios/03-stress.js

# スパイクテスト
k6 run performance/scenarios/04-spike.js
```

### ブラウザテスト

```powershell
# フロントエンド・バックエンド両方が起動している状態で実行
k6 run --browser performance/scenarios/05-browser.js
```

### ステージング環境向け

```powershell
$env:BASE_URL = "http://staging.example.com"
$env:FRONTEND_URL = "http://staging.example.com"
k6 run performance/scenarios/02-load.js
```

## 結果の読み方

実行後のコンソール出力例:

```
✓ タイムライン: 200
✓ 単一投稿: 200 or 404

http_req_duration........: avg=120ms  min=45ms   med=98ms   max=850ms  p(90)=210ms p(95)=350ms
timeline_latency.........: avg=145ms  min=60ms   med=120ms  max=900ms  p(90)=250ms p(95)=380ms

thresholds:
  ✓ timeline_latency p(95)<400   ← 緑 = SLO クリア
  ✗ http_req_duration p(95)<150  ← 赤 = SLO 違反（要調査）
```

- **緑（✓）**: SLO をクリア
- **赤（✗）**: SLO 違反 → 該当エンドポイントの最適化が必要

## SLO（サービスレベル目標）

| エンドポイント | ベースライン p95 | 負荷テスト p95 |
|---|---|---|
| ログイン | 800ms | 1200ms |
| タイムライン `GET /api/posts` | 400ms | 500ms |
| 単一投稿 `GET /api/posts/{id}` | 150ms | 250ms |
| 投稿作成 `POST /api/posts` | 300ms | 400ms |
| ユーザープロフィール `GET /api/users/{id}` | 150ms | 250ms |
| ユーザー検索 `GET /api/users?q=` | 200ms | 300ms |
| いいね `POST /api/posts/{id}/likes` | 200ms | 300ms |
| タイムライン LCP（ブラウザ） | 2500ms | — |

## クリーンアップ

各シナリオの `teardown()` がテスト中に作成したデータを自動削除する。

シードデータも含めて全削除したい場合:

```powershell
# API 経由で削除（perf_user の投稿・フォロー関係を削除）
k6 run --vus 1 --iterations 1 performance/cleanup.js

# DB ボリュームごとリセット（最も確実）
docker compose down -v
docker compose up -d
```

## よくある問題

**`k6: command not found`**
→ `winget install k6` を実行。PowerShell を再起動して PATH を反映させる。

**`login: status 200` のチェックが失敗する**
→ seed.js が実行されていない。`k6 run --vus 1 --iterations 1 performance/seed.js` を実行。

**ストレステストが途中で ABORTED される**
→ 正常動作。p95 > 2000ms または エラー率 > 5% に達したことを意味する。
   中断時の VU 数がシステムの限界点。

**05-browser.js でブラウザが開かない**
→ フロントエンド（port 5173）が起動しているか確認。
   `npm run dev` が動いていることを確認する。

**`ECONNREFUSED` エラー**
→ バックエンドまたは PostgreSQL が起動していない。
   `docker compose up -d` → `./gradlew bootRun` の順に起動する。
