---
name: quality-check
description: Run frontend (Vitest) and backend (Gradle) tests for RaiseTimeLine. Use when the user asks for quality checks, test runs, or before creating a PR.
---

# quality-check

Frontend（Vitest）と Backend（Gradle）のテストを順番に実行し、結果を報告する。

## 実行ステップ

### Step 1: Frontend テスト（Vitest）

```bash
cd frontend && npm test
```

- テスト結果（PASS/FAIL件数）を報告する
- 失敗したテストがあれば、テスト名とエラー内容を列挙する

### Step 2: Backend テスト（Gradle）

```bash
cd backend && ./gradlew test
```

- テスト結果（PASS/FAIL件数）を報告する
- 失敗したテストがあれば、クラス名・テスト名・エラー内容を列挙する
- Gradleは初回に時間がかかる場合があることをユーザーに伝える

## 結果の報告

以下の形式でチェック結果を報告する:

```
## 品質チェック結果

### Frontend（Vitest）
- ✅ 全66件 PASS  /  ❌ N件 FAIL: <テスト名・エラー内容>

### Backend（Gradle）
- ✅ 全176件 PASS  /  ❌ N件 FAIL: <クラス名・テスト名・エラー内容>
```

すべてパスした場合:「Frontend・Backend ともに全テストがパスしました。PRを作成できる状態です。」と報告する。

失敗があった場合:「N件のテストが失敗しています。PRを作成する前に修正してください。」と報告し、修正を提案する。
