# RaiseTimeLine 認証機能定義書

作成日: 2026-05-13
最終更新日: 2026-05-22

---

## 1. 概要

RaiseTimeLine の認証は **JWT（JSON Web Token）** を用いたステートレス認証を採用する。  
フロントエンド（React SPA）とバックエンド（Spring Boot REST API）が分離した構成に適合させるため、Cookie セッションではなく Bearer トークン方式を採用する。

---

## 2. 認証方式

| 項目 | 内容 |
|------|------|
| 方式 | JWT（Bearer Token） |
| アルゴリズム | HS256（HMAC-SHA256） |
| トークン格納場所 | フロントエンド：`localStorage` |
| トークン送信方法 | `Authorization: Bearer <token>` ヘッダー |
| 実装ライブラリ | Spring Security + JJWT（バックエンド） |

---

## 3. 対象機能

| 機能ID | 機能名 | 本定義書での対応 |
|--------|--------|----------------|
| F-01 | ユーザー登録 | §5.1 参照 |
| F-02 | ログイン | §5.2 参照 |
| F-03 | ログアウト | §5.3 参照 |

---

## 4. JWT トークン仕様

### 4.1 ペイロード（クレーム）

| クレーム | 型 | 内容 |
|---------|-----|------|
| `sub` | String | ユーザーID（`users.id`） |
| `iat` | Long | 発行日時（Unix秒） |
| `exp` | Long | 有効期限（Unix秒） |

> `email` や `username` 等の変更可能な情報はペイロードに含めない。  
> 必要な場合は `/api/me` エンドポイントで都度取得する。

### 4.2 トークン有効期限

| 区分 | 有効期間 | 設定キー |
|------|---------|---------|
| アクセストークン | **15分**（900秒） | `app.jwt.expiration-ms: 900000` |
| リフレッシュトークン | **7日**（604800秒） | `app.jwt.refresh-expiration-ms: 604800000` |

> アクセストークンは短命（15分）に設計し、セキュリティリスクを最小化する。  
> 有効期限切れ時はリフレッシュトークンで新しいアクセストークンを取得する（`POST /api/auth/refresh`）。  
> リフレッシュトークンも期限切れの場合はログイン画面へリダイレクトする。

### 4.3 署名鍵

| 項目 | 内容 |
|------|------|
| 鍵の種類 | 256 bit 以上のランダム文字列 |
| 管理方法 | 環境変数 `JWT_SECRET` で注入（ハードコード禁止） |
| ローカル開発 | `.env` ファイルまたは `application-local.yml` で設定 |
| 本番環境 | AWS Secrets Manager または EC2 の環境変数で管理 |

---

## 5. 機能詳細

### 5.1 ユーザー登録（F-01）

#### API

| 項目 | 内容 |
|------|------|
| エンドポイント | `POST /api/auth/register` |
| 認証 | 不要（Public） |

#### リクエスト

```json
{
  "email": "user@example.com",
  "username": "raisetaro",
  "password": "P@ssw0rd123",
  "passwordConfirm": "P@ssw0rd123"
}
```

| フィールド | 型 | 必須 | バリデーション |
|-----------|-----|------|--------------|
| email | String | ○ | RFC5322 形式、255 文字以内、DB 未登録 |
| username | String | ○ | 1〜50 文字 |
| password | String | ○ | 8〜72 文字、英数字・記号を含む |
| passwordConfirm | String | ○ | `password` と一致 |

#### 処理フロー

```
1. リクエストボディのバリデーション
2. email の重複チェック（DB）
3. パスワードを BCrypt でハッシュ化（strength=10）
4. users テーブルへ INSERT
5. JWT を生成して返却
```

#### レスポンス（成功 201）

```json
{
  "token": "<アクセストークン（15分）>",
  "refreshToken": "<リフレッシュトークン（7日）>",
  "user": {
    "id": 1,
    "username": "raisetaro",
    "avatarUrl": null
  }
}
```

#### エラーレスポンス

| ステータス | コード | 条件 |
|-----------|--------|------|
| 400 | `VALIDATION_ERROR` | バリデーション失敗（詳細は `errors` フィールドに含む） |
| 409 | `EMAIL_ALREADY_EXISTS` | メールアドレス重複 |

---

### 5.2 ログイン（F-02）

#### API

| 項目 | 内容 |
|------|------|
| エンドポイント | `POST /api/auth/login` |
| 認証 | 不要（Public） |

#### リクエスト

```json
{
  "email": "user@example.com",
  "password": "P@ssw0rd123"
}
```

| フィールド | 型 | 必須 | バリデーション |
|-----------|-----|------|--------------|
| email | String | ○ | 空でないこと |
| password | String | ○ | 空でないこと |

#### 処理フロー

```
1. email で users テーブルを検索
2. BCrypt で入力パスワードと password_hash を比較
3. 一致した場合、JWT を生成して返却
4. 不一致の場合、401 を返却（email/password どちらが誤りかは明示しない）
```

#### レスポンス（成功 200）

```json
{
  "token": "<アクセストークン（15分）>",
  "refreshToken": "<リフレッシュトークン（7日）>",
  "user": {
    "id": 1,
    "username": "raisetaro",
    "avatarUrl": "https://s3.amazonaws.com/..."
  }
}
```

#### エラーレスポンス

| ステータス | コード | 条件 |
|-----------|--------|------|
| 400 | `VALIDATION_ERROR` | email または password が空 |
| 401 | `INVALID_CREDENTIALS` | 認証失敗（email 不存在 or パスワード不一致） |

---

### 5.3 ログアウト（F-03）

#### API

| 項目 | 内容 |
|------|------|
| エンドポイント | `POST /api/auth/logout` |
| 認証 | 不要（リフレッシュトークンをボディで受け取る） |

#### リクエスト

```json
{
  "refreshToken": "<リフレッシュトークン>"
}
```

#### 処理フロー

```
1. リクエストボディのリフレッシュトークンを検証
2. DB上のリフレッシュトークンハッシュを無効化（NULLクリア）
3. フロントエンドが localStorage からトークンを削除
4. ログイン画面へリダイレクト
```

#### レスポンス（成功 204 No Content）

> サーバー側でリフレッシュトークンを無効化することで、ログアウト後はトークンの再利用を防止する。  
> アクセストークン（15分）は有効期限切れを待つが、短命設計のため実害は限定的。

---

### 5.4 トークンリフレッシュ（F-04）

#### API

| 項目 | 内容 |
|------|------|
| エンドポイント | `POST /api/auth/refresh` |
| 認証 | 不要（リフレッシュトークンをボディで受け取る） |

#### リクエスト

```json
{
  "refreshToken": "<リフレッシュトークン>"
}
```

#### 処理フロー

```
1. リフレッシュトークンを DB のハッシュと照合
2. 有効期限を確認（refreshTokenExpiresAt）
3. 新しいアクセストークンを生成して返却
4. リフレッシュトークン自体は更新しない（ローテーションなし）
```

#### レスポンス（成功 200）

```json
{
  "token": "<新しいアクセストークン（15分）>",
  "refreshToken": "<元のリフレッシュトークン>",
  "user": { ... }
}
```

#### エラーレスポンス

| ステータス | 条件 |
|-----------|------|
| 401 | リフレッシュトークンが無効・期限切れ・ログアウト済み |

---

### 5.5 ログイン中ユーザー取得（補助 API）

認証済みユーザーの情報をフロントエンドが取得するためのエンドポイント。

| 項目 | 内容 |
|------|------|
| エンドポイント | `GET /api/me` |
| 認証 | 必要（Bearer Token） |

#### レスポンス（成功 200）

```json
{
  "id": 1,
  "username": "raisetaro",
  "email": "user@example.com",
  "bio": "よろしくお願いします",
  "avatarUrl": "https://s3.amazonaws.com/..."
}
```

#### エラーレスポンス

| ステータス | コード | 条件 |
|-----------|--------|------|
| 401 | `UNAUTHORIZED` | トークンなし・改ざん・有効期限切れ |

---

## 6. Spring Security 設定方針

### 6.1 アクセス制御

| パス | 認証 | 説明 |
|------|------|------|
| `POST /api/auth/register` | 不要 | 新規登録 |
| `POST /api/auth/login` | 不要 | ログイン |
| `GET /api/users/{id}` | 不要 | プロフィール閲覧（将来的に公開にする場合） |
| その他 `/api/**` | 必要 | 全認証済みエンドポイント |

### 6.2 JWT フィルター処理フロー

```
リクエスト
  ↓
JwtAuthenticationFilter（OncePerRequestFilter）
  ↓ Authorization ヘッダーを取得
  ↓ "Bearer " プレフィックスを除去してトークンを取り出す
  ↓ トークンを検証（署名・有効期限）
  ↓ sub クレームから userId を取得
  ↓ SecurityContextHolder に Authentication をセット
Spring Security の残りのフィルターチェーン
  ↓
コントローラー（@AuthenticationPrincipal で userId を受け取る）
```

### 6.3 パスワードハッシュ

| 項目 | 内容 |
|------|------|
| アルゴリズム | BCrypt |
| コストファクター（strength） | 10 |
| 実装 | Spring Security の `BCryptPasswordEncoder` |

---

## 7. フロントエンド認証フロー

### 7.1 トークン管理

| 操作 | タイミング | 処理 |
|------|-----------|------|
| 保存 | ログイン成功 / 会員登録成功 | `localStorage.setItem('token', jwt)` |
| 読み取り | API リクエスト毎 | Axios インターセプターで自動付与 |
| 削除 | ログアウト / 401 受信時 | `localStorage.removeItem('token')` |

### 7.2 Axios インターセプター

```typescript
// リクエストインターセプター
axios.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// レスポンスインターセプター（401 ハンドリング）
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

### 7.3 ルートガード（React Router）

| ルート種別 | 動作 |
|-----------|------|
| 要認証ルート（`/timeline` 等） | 未ログイン時は `/login` へリダイレクト |
| 非認証ルート（`/login`, `/register`） | ログイン済み時は `/timeline` へリダイレクト |

---

## 8. バリデーション詳細

### 8.1 メールアドレス

| ルール | 内容 |
|--------|------|
| 形式 | RFC5322 準拠（`@` を含む有効な形式） |
| 最大長 | 255 文字 |
| 一意性 | DB の UNIQUE 制約でも保証 |
| 大文字小文字 | 保存前に小文字に正規化する |

### 8.2 ユーザー名

| ルール | 内容 |
|--------|------|
| 最小長 | 1 文字 |
| 最大長 | 50 文字 |
| 使用可能文字 | 制限なし（絵文字も可） |

### 8.3 パスワード

| ルール | 内容 |
|--------|------|
| 最小長 | 8 文字 |
| 最大長 | 72 文字（BCrypt の入力上限） |
| 文字種 | 英字（大文字・小文字）・数字・記号を各 1 文字以上含むこと |
| 保存形式 | BCrypt ハッシュのみ保存（平文は保持しない） |

---

## 9. セキュリティ考慮事項

| 項目 | 対策 |
|------|------|
| ブルートフォース攻撃 | MVP スコープ外。将来的にレートリミット（Spring Security + Bucket4j）を導入する |
| JWT 漏洩 | HTTPS 必須（本番環境）。`localStorage` の XSS リスクは認識の上、MVP では許容する |
| CSRF | JWT 認証（Cookie 未使用）のため CSRF トークン不要 |
| パスワードリスト攻撃 | 認証失敗時のエラーメッセージを統一（どちらが誤りかを明示しない） |
| SQL インジェクション | Spring Data JPA / パラメータバインディングにより防止 |
| XSS | React の JSX エスケープ + CSP ヘッダー（将来対応） |

---

## 10. エラーレスポンス共通フォーマット

全 API のエラーレスポンスは以下の形式に統一する。

```json
{
  "code": "ERROR_CODE",
  "message": "エラーの説明（日本語または英語）",
  "errors": [
    {
      "field": "email",
      "message": "メールアドレスの形式が正しくありません"
    }
  ]
}
```

> `errors` フィールドはバリデーションエラー（400）のみに付与する。

---

## 11. 関連ドキュメント

| ドキュメント | パス |
|------------|------|
| 機能一覧・機能定義書 | [docs/features.md](features.md) |
| データモデル・ER図 | [docs/data-model.md](data-model.md) |
| 技術スタック | [docs/tech-stack.md](tech-stack.md) |
| 非機能要件 | [docs/non-functional.md](non-functional.md) |
