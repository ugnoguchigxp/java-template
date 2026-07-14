# Java 25 PostgreSQL Template

Java 25 を実行対象にした、Spring Boot 4 + MyBatis 4 + PostgreSQL の最小構成テンプレートです。フロントエンドは `../hono-standard` の React/Vite UI、Design System、Showcase をコピーして使用します。SSG/SSR と Docker は含めません。

## 前提

- Java 25（`JAVA25_HOME` を設定。Temurin 25.0.2 など）
- Bun
- Gradle Wrapper 9.1.0 以上を使用します
- PostgreSQL 14以上と `DATABASE_URL=jdbc:postgresql://...` を設定します

## 開発

```sh
cp .env.example .env
bun install
bun run bootstrap
bun run dev
```

`bun run dev` はViteとSpring Bootを同時に起動します。バックエンドだけを起動する場合は `bun run start` を使います。既定の SQLite ファイルは `data/sqlite.db`、API は `http://127.0.0.1:8080/api` です。

管理者を作成するには、パスワードを標準入力から渡します。

```sh
printf '%s\n' 'password123456' | bun run auth:create-admin -- --email admin@example.com --name Admin --password-stdin
```

## 検証

```sh
bun run verify        # TypeScript/Frontend + Java 25 compile/test/coverage/JAR
bun run test:e2e     # Playwright (SQLite の一時 DB を使用)
bun run contract:check
```

Java 側の主要なエンドポイントは `/api/health`、`/api/csrf`、`/api/auth/login`、`/api/auth/me`、`/api/auth/refresh`、`/api/auth/logout`、`/api/protected/profile` です。認証は HttpOnly access/refresh Cookie と CSRF double-submit を使います。

## 構成

- `src/main/java`: Java 25 対応の API、認証、PostgreSQL/MyBatis 境界
- `db/migration`: PostgreSQL SQL マイグレーション
- `web`: hono-standard からコピーした UI（Home/Login/Protected/Showcase）
- `shared/schemas`: UI と API で共有する Zod スキーマ
- `api-contract/openapi.yaml`: API 契約
