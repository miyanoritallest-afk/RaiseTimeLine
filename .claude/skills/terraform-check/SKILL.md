---
name: terraform-check
description: Run quality checks on Terraform code in infra/. Validates formatting, syntax, and best practices using terraform fmt, validate, and tflint.
---

# terraform-check

`infra/` 配下の Terraform コードに対して品質チェックを実行する。

## 前提条件の確認

```bash
terraform version
tflint --version
```

どちらかがインストールされていない場合は、その旨をユーザーに伝えて該当ステップをスキップする。

## 実行ステップ

### Step 1: terraform fmt（フォーマットチェック）

```bash
cd infra
terraform fmt -check -recursive
```

- 差分がある場合は自動修正する:
  ```bash
  terraform fmt -recursive
  ```
- 修正したファイルを報告する

### Step 2: terraform validate（構文・依存関係チェック）

```bash
cd infra
terraform init -backend=false
terraform validate
```

- `Success! The configuration is valid.` が出ればOK
- エラーが出た場合は内容を報告して修正を提案する

### Step 3: tflint（ベストプラクティスチェック）

tflint がインストールされている場合のみ実行:

```bash
cd infra
tflint --init
tflint
```

- 警告・エラーをすべて報告する
- `aws_instance` の非推奨引数や未使用変数などを検出する

## 結果の報告

以下の形式でチェック結果を報告する:

```
## Terraform 品質チェック結果

### fmt
- ✅ フォーマット問題なし  /  ⚠️ 修正あり（修正済み）: <ファイル名>

### validate
- ✅ 構文エラーなし  /  ❌ エラー: <内容>

### tflint
- ✅ 問題なし  /  ⚠️ 警告: <内容>  /  （スキップ: tflint 未インストール）
```

すべてパスした場合:「Terraform コードの品質チェックがすべてパスしました。」と報告する。
