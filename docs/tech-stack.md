# RaiseTimeLine 技術スタック

作成日: 2026-05-12
最終更新日: 2026-05-12

---

## 1. 使用技術一覧

### バックエンド

| 役割 | 技術 | バージョン |
|------|------|----------|
| 言語 | Java | 21（LTS） |
| フレームワーク | Spring Boot | 3.x |
| ビルドツール | Gradle | 8.x |
| ORM | Spring Data JPA / Hibernate | Spring Boot同梱 |
| DBマイグレーション | Flyway | Spring Boot同梱 |
| 認証 | Spring Security + JWT | Spring Boot同梱 |
| 画像アップロード | AWS SDK for Java (S3) | 2.x |

### フロントエンド

| 役割 | 技術 | バージョン |
|------|------|----------|
| 言語 | TypeScript | 5.x |
| フレームワーク | React | 18.x |
| ビルドツール | Vite | 5.x |
| HTTPクライアント | Axios | 1.x |
| スタイリング | CSS Modules | — |

### インフラ・ストレージ

| 役割 | 技術 | 備考 |
|------|------|------|
| データベース | PostgreSQL | 16.x |
| 画像ストレージ | AWS S3 | — |
| サーバー | AWS EC2 | 構築予定 |
| データベースサーバー | AWS RDS (PostgreSQL) | 構築予定 |
| ロードバランサー | AWS ALB | 構築予定 |
| ローカル開発DB | Docker Compose (PostgreSQL) | — |

---

## 2. 各技術の採用理由

#### Java 21 LTS
学習目的として実務で広く使われる言語を採用。LTSバージョンを選択することで長期間の安定サポートを確保する。

#### Spring Boot 3.x
Java製Webアプリケーションのデファクトスタンダード。DIコンテナ・ORM・セキュリティ・マイグレーションが統合されており、学習効率が高い。

#### JWT認証
ステートレスな認証方式であり、フロントエンドとバックエンドを分離したSPA構成に適している。

#### React 18.x + TypeScript
SPAとして動作する軽快なUIを実現する。TypeScriptで型安全性を確保し、バグを早期に検出する。

#### Vite 5.x
高速なHMR（ホットモジュールリプレースメント）を提供し、開発体験を向上させる。

#### PostgreSQL
オープンソースのRDBMSとして実績が豊富。AWS RDSでそのまま使用できる互換性がある。

#### AWS S3
画像ファイルをサーバーとは独立して管理できる。スケーラビリティが高く、URLで直接参照できる。

#### Flyway
SQLベースのDBマイグレーション管理ツール。スキーマ変更の履歴をコードとして管理できる。

---

## 3. アーキテクチャ概要

```
[ブラウザ（React）]
        ↓ HTTP / REST API
[Spring Boot（API Server）]
        ↓
[PostgreSQL（RDS）]

[React] ←→ [AWS S3]（画像の直接アップロード / 表示）
```

- フロントエンドは React SPA として動作し、Spring Boot REST API を呼び出す
- 画像はクライアントから直接 S3 へアップロードするか、サーバー経由でアップロードする
- 認証には JWT を使用し、Authorization ヘッダーで各リクエストに付与する

---

## 4. CORS 戦略

| 環境 | 対応方法 |
|------|---------|
| ローカル開発 | Vite のプロキシ設定（`/api` → `localhost:8080`）でCORSを回避 |
| 本番環境 | Spring Boot の CorsConfig でフロントエンドのオリジンを許可 |

---

## 5. ローカル開発環境

### 必要なソフトウェア

| ソフトウェア | バージョン | 用途 |
|------------|----------|------|
| Java | 21 | バックエンド実行 |
| Node.js | 20.x LTS | フロントエンド実行 |
| Docker Desktop | 最新版 | ローカルDB起動 |

### 接続情報（ローカル）

| サービス | ホスト | ポート |
|---------|--------|--------|
| Spring Boot API | localhost | 8080 |
| React Dev Server | localhost | 5173 |
| PostgreSQL | localhost | 5432 |

### 起動手順

```bash
# 1. DBを起動（Docker Compose）
docker compose up -d

# 2. バックエンド起動
cd backend
./gradlew bootRun

# 3. フロントエンド起動
cd frontend
npm install
npm run dev
```
