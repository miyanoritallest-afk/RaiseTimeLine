---
name: performance-test
description: Run k6 performance tests for RaiseTimeLine. Use ONLY when the user explicitly asks to run performance tests, load tests, stress tests, or benchmark the API. Do NOT trigger automatically — must be explicitly requested.
---

# performance-test

k6 を使ってパフォーマンステストを実行し、結果を報告する。
**必ず手動で明示的に指示された場合のみ実行すること。自動実行・品質チェックの一環としての自動起動は禁止。**

## 前提条件チェック

### k6 インストール確認

```powershell
$env:PATH = [System.Environment]::GetEnvironmentVariable("PATH","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("PATH","User")
k6 version
```

インストールされていない場合:
```powershell
winget install --id GrafanaLabs.k6 --accept-package-agreements --accept-source-agreements
```

### サーバー起動確認

ポート 8080（バックエンド）と 5432（PostgreSQL）が起動しているか確認:

```powershell
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -in 8080, 5432 } | Select-Object LocalPort
```

起動していない場合は `/start-servers` を先に実行するようユーザーに案内する。

### テストデータ確認

seed データの有無を確認するため、ログインテストを 1 本だけ実行する:

```powershell
$env:PATH = [System.Environment]::GetEnvironmentVariable("PATH","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("PATH","User")
Set-Location "c:\宮本憲秀\CursorProjects\RaiseTimeLine"
k6 run --vus 1 --iterations 1 --env BASE_URL=http://localhost:8080 -e "QUICK_CHECK=true" performance/scenarios/01-baseline.js
```

401 や接続エラーが出た場合は seed を実行する:
```powershell
$env:PATH = [System.Environment]::GetEnvironmentVariable("PATH","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("PATH","User")
Set-Location "c:\宮本憲秀\CursorProjects\RaiseTimeLine"
k6 run --vus 1 --iterations 1 performance/seed.js
```

## 実行するシナリオを確認する

ユーザーに何を実行するか確認する（指定がない場合は以下を提案）:

| 番号 | シナリオ | 所要時間 | 内容 |
|---|---|---|---|
| 1 | `01-baseline` | 約3分 | 1ユーザーで各エンドポイントの素の速度を計測 |
| 2 | `02-load` | 約9分 | 最大50同時ユーザーで負荷をかける |
| 3 | `03-stress` | 最大16分 | 限界点（何同時ユーザーで壊れるか）を探索 |
| 4 | `04-spike` | 約4分 | 瞬間的な大量アクセスへの耐性を確認 |
| 5 | `05-browser` | 約3〜5分 | Chrome でページ表示速度（Core Web Vitals）を計測 |
| ALL | 全シナリオ | 約35〜40分 | 上記すべてを順番に実行 |

## シナリオ実行コマンド

すべて以下のように `$env:PATH` を設定してから実行する（k6 が PATH に入っていない場合があるため）:

```powershell
$env:PATH = [System.Environment]::GetEnvironmentVariable("PATH","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("PATH","User")
Set-Location "c:\宮本憲秀\CursorProjects\RaiseTimeLine"
```

### 01-baseline
```powershell
k6 run performance/scenarios/01-baseline.js
```

### 02-load
```powershell
k6 run performance/scenarios/02-load.js
```

### 03-stress
```powershell
k6 run performance/scenarios/03-stress.js
```
※ exit code 99 は正常終了（abortOnFail による限界点検出）

### 04-spike
```powershell
k6 run performance/scenarios/04-spike.js
```

### 05-browser（フロントエンドも起動している必要あり）
```powershell
k6 run --browser performance/scenarios/05-browser.js
```

### 全シナリオ（順番に実行）
```powershell
k6 run performance/scenarios/01-baseline.js
k6 run performance/scenarios/02-load.js
k6 run performance/scenarios/03-stress.js
k6 run performance/scenarios/04-spike.js
```

## 結果の報告

各シナリオ終了後、以下の形式で報告する:

```
## パフォーマンステスト結果

### 01-baseline
| エンドポイント | SLO | 実測 p95 | 判定 |
|---|---|---|---|
| ログイン        | <800ms | Xms | ✅/❌ |
| タイムライン    | <400ms | Xms | ✅/❌ |
| 単一投稿        | <150ms | Xms | ✅/❌ |
| ユーザー        | <150ms | Xms | ✅/❌ |
| フォロワー一覧  | <200ms | Xms | ✅/❌ |
エラー率: X%

### 02-load（最大50VU）
...

### 03-stress（限界点）
- 限界 VU 数: 約 X VU でエラー率 5% 超
- 限界原因: エラー率超過 / 応答時間超過

### 04-spike
...
```

全閾値グリーンの場合:「全シナリオ SLO クリア。パフォーマンス問題は検出されませんでした。」と報告する。
閾値違反がある場合:「X シナリオで SLO 違反。<エンドポイント名> の p95 が <閾値> を超えています。HikariCP 接続プールサイズの増加や SQL クエリ最適化を検討してください。」と報告する。

## テスト後のクリーンアップ

各シナリオの `teardown()` がテスト中に作成した投稿を自動削除する。
seed データも削除したい場合はユーザーに確認してから:

```powershell
$env:PATH = [System.Environment]::GetEnvironmentVariable("PATH","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("PATH","User")
Set-Location "c:\宮本憲秀\CursorProjects\RaiseTimeLine"
k6 run --vus 1 --iterations 1 performance/cleanup.js
```
