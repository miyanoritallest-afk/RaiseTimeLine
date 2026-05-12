---
name: doc-sync
description: Check for divergences between the current implementation and docs/ documentation. Produces a diff report of what is out of date, then asks the user whether to auto-fix the docs to match the implementation.
---

# doc-sync

実装を正として、`docs/` 配下のドキュメントとの差異を検出し、必要に応じて自動修正する。

## Phase 1: 実装の現状を調査する

以下のファイルを読み込んで実装の現状を把握する：

### フロントエンド
- `frontend/package.json` — 依存パッケージとバージョン
- `frontend/src/types.ts` — 型定義（User, Post, Comment, Like, Follow 等）
- `frontend/src/components/Timeline.tsx` — タイムライン表示仕様
- `frontend/src/components/PostCard.tsx` — 投稿カードの表示仕様（いいね、コメント数、画像）
- `frontend/src/components/PostForm.tsx` — 投稿フォーム UI（文字数制限、画像選択）
- `frontend/src/components/CommentList.tsx` — コメント一覧・フォーム UI

### バックエンド
- `backend/build.gradle.kts` — 依存ライブラリとプラグイン（バージョン含む）
- `backend/src/main/java/com/raisetimeline/backend/entity/User.java` — User エンティティ
- `backend/src/main/java/com/raisetimeline/backend/entity/Post.java` — Post エンティティ
- `backend/src/main/java/com/raisetimeline/backend/entity/Comment.java` — Comment エンティティ
- `backend/src/main/java/com/raisetimeline/backend/entity/Like.java` — Like エンティティ
- `backend/src/main/java/com/raisetimeline/backend/entity/Follow.java` — Follow エンティティ
- `backend/src/main/java/com/raisetimeline/backend/controller/PostController.java` — REST API エンドポイント
- `backend/src/main/resources/db/migration/V1__create_tables.sql` — テーブル定義

### ドキュメント
- `docs/requirements.md`
- `docs/features.md`
- `docs/screens.md`
- `docs/data-model.md`
- `docs/non-functional.md`
- `docs/tech-stack.md`
- `docs/infrastructure.md`

## Phase 2: 差異レポートを作成する

以下の観点でドキュメントと実装を比較し、差異をまとめる：

| チェック項目 | 確認ポイント |
|-------------|-------------|
| **tech-stack.md** | ライブラリ名・バージョン、追加/削除されたプラグインや依存関係 |
| **features.md** | UC の基本フロー（投稿方法、いいね操作、フォロー操作等）が実装と一致しているか |
| **screens.md** | UI コンポーネントの操作方法、エラー表示方法、画面遷移図 |
| **data-model.md** | テーブル定義・カラム型・制約が V1 マイグレーションと一致しているか |
| **requirements.md** | 技術スタック概要（フレームワーク名等）が実装と一致しているか |
| **non-functional.md** | 保存先・ORM・デプロイ方法の記述が実装と一致しているか |

差異がある場合は以下の形式でレポートする：

```
## ドキュメント差異レポート

### docs/tech-stack.md
- [差異] Spring Boot のバージョンが「3.x」と記載されているが、実際は 3.3.0 で導入済み

### docs/data-model.md
- [差異] posts テーブルに content_length カラムが追加されているがドキュメントに未記載

...（差異がなければ「差異なし」と記載）
```

差異がない場合は「すべてのドキュメントは実装と一致しています。」と報告して終了する。

## Phase 3: 自動修正の確認

差異がある場合、ユーザーに確認する：

```
上記の差異を自動修正しますか？
- はい：実装に合わせてドキュメントを修正します
- いいえ：レポートのみで終了します
```

ユーザーが「はい」を選択した場合のみ、Phase 4 を実行する。

## Phase 4: ドキュメントを自動修正する

差異ごとに Edit ツールで該当箇所を修正する。修正の原則：

1. **実装を正とする** — コードの挙動がドキュメントに優先する
2. **未実装機能の記述は変更しない** — 将来実装予定の機能記述はそのまま残す
3. **最小変更** — 差異がある箇所のみ修正し、周辺の文章は変えない
4. **日本語を維持** — ドキュメントの言語・文体をそのまま維持する

修正完了後、変更したファイルと変更箇所の一覧を報告する。
