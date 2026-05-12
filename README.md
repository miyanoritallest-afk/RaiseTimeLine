# RaiseTimeLine

X（旧Twitter）ライクなシンプルなタイムライン SNS アプリ。React + Spring Boot で構築する学習目的プロジェクト。
テキスト投稿・画像添付・いいね・コメント・フォローといった SNS の基本機能を、複数ユーザーが利用できる環境で実装する。

インプレッション表示・リツイートは意図的にスコープ外とし、シンプルな構成を維持している。

## 機能概要

| カテゴリ | 機能 |
|---------|------|
| 認証 | ユーザー登録・ログイン・ログアウト（JWT） |
| タイムライン | 全体表示（おすすめ）・フォロー中表示をタブで切り替え |
| 投稿 | テキスト投稿（最大 280 文字）・画像添付（最大 4 枚、AWS S3 保存）・編集・削除 |
| コメント | コメント投稿・編集・削除・コメント数表示 |
| いいね | いいね・取り消し・いいね数表示 |
| フォロー | フォロー・解除・フォロー中 / フォロワー一覧 |
| ユーザー検索 | ユーザー名（部分一致）で検索・検索結果にフォロー状態を表示 |
| プロフィール | アバター画像・ユーザー名・自己紹介・投稿一覧の表示と編集 |

15 のユースケース・全機能一覧は [docs/features.md](docs/features.md) を参照。

## 技術スタック

| レイヤー | 技術 | バージョン |
|---------|------|----------|
| 言語（バックエンド） | Java | 21 LTS |
| バックエンド | Spring Boot | 3.x |
| ビルドツール | Gradle | 8.x |
| 認証 | Spring Security + JWT | Spring Boot 同梱 |
| DB マイグレーション | Flyway | Spring Boot 同梱 |
| 画像ストレージ | AWS SDK for Java (S3) | 2.x |
| UI ライブラリ | React | 18.x |
| フロントエンドビルド | Vite | 5.x |
| 言語（フロントエンド） | TypeScript | 5.x |
| HTTP クライアント | Axios | 1.x |
| データベース | PostgreSQL | 16 |
| インフラ（予定） | AWS EC2 / RDS / ALB / S3 | — |

採用理由・アーキテクチャ詳細は [docs/tech-stack.md](docs/tech-stack.md) を参照。

## 必要な環境

- **Java 21**（JDK — `./gradlew` が使用）
- **Node.js 20+** と **npm**
- **Docker Desktop**（Docker Compose で PostgreSQL を起動）
- **Git**

## ローカル開発セットアップ

### 1. PostgreSQL を起動

```bash
docker compose up -d
```

`docker-compose.yml` の設定で PostgreSQL 16 をポート 5432 で起動する。

### 2. バックエンドを起動

```bash
cd backend
./gradlew bootRun
```

API は `http://localhost:8080` で利用可能。起動時に Flyway が DB マイグレーションを自動実行する。

### 3. フロントエンドを起動

```bash
cd frontend
npm install   # 初回のみ
npm run dev
```

UI は `http://localhost:5173` で利用可能。Vite が `/api/*` リクエストをバックエンド（8080）へプロキシする。

### ポート一覧

| サービス | ポート | 設定ファイル |
|---------|--------|------------|
| PostgreSQL | 5432 | `docker-compose.yml` |
| バックエンド（Spring Boot） | 8080 | `backend/src/main/resources/application.properties` |
| フロントエンド（Vite） | 5173 | Vite デフォルト |

ポートは固定。変更不可。Vite プロキシと Spring Boot CORS 設定がこの値に依存している。

## プロジェクト構造

```
RaiseTimeLine/
├── backend/                        # Spring Boot アプリケーション
│   ├── src/main/java/              # Java ソース（controllers, services, repositories）
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── db/migration/           # Flyway SQL マイグレーション（V1__, V2__, ...）
│   ├── build.gradle.kts
│   └── Dockerfile
├── frontend/                       # React + Vite アプリケーション
│   ├── src/                        # TypeScript ソース（components, contexts, api）
│   ├── vite.config.ts              # Vite 設定 + /api プロキシ → :8080
│   └── package.json
├── infra/                          # インフラ設定（Terraform 等）
├── docs/                           # プロジェクトドキュメント
├── docker-compose.yml              # PostgreSQL 定義（ローカル開発用）
└── CLAUDE.md                       # 開発ワークフロールール
```

## ドキュメント

| ドキュメント | 内容 |
|-------------|------|
| [docs/requirements.md](docs/requirements.md) | システム概要・機能スコープ（IN/OUT）・画面概要 |
| [docs/features.md](docs/features.md) | 24 機能・15 ユースケース（UC-01〜UC-15）の詳細定義 |
| [docs/screens.md](docs/screens.md) | 7 画面のワイヤーフレーム・エラー仕様・画面遷移図（Mermaid） |
| [docs/data-model.md](docs/data-model.md) | ER 図（Mermaid）・6 テーブル定義（users, posts, post_images, comments, likes, follows） |
| [docs/tech-stack.md](docs/tech-stack.md) | 使用技術・バージョン・採用理由・ローカル起動手順 |
| [docs/infrastructure.md](docs/infrastructure.md) | AWS 構成図（ALB / EC2 / RDS / S3）・セキュリティグループ設計 |
| [docs/non-functional.md](docs/non-functional.md) | パフォーマンス・セキュリティ・ブラウザ対応・アクセシビリティ |
| [CLAUDE.md](CLAUDE.md) | Git ワークフロー・コミット形式・コーディングルール |

## データモデル

6 テーブル構成：

```
users ──< posts ──< post_images
  │         │
  │         ├──< comments
  │         └──< likes
  │
  └──< follows >── users（自己参照）
```

`post` の主なフィールド: `content`（最大 280 文字）、`user_id`
`post_images` の主なフィールド: `image_url`（S3 URL）、`display_order`（1〜4）
`follows` の制約: `UNIQUE(follower_id, following_id)`、`CHECK(follower_id <> following_id)`

詳細は [docs/data-model.md](docs/data-model.md) の ER 図を参照。
