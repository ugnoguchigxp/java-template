# Java 8 + SQLite Template Implementation Plan

## 0. この計画の目的

`../hono-standard` の認証、React画面、セキュリティ、bootstrap、検証パイプラインを参考にしつつ、Java 8 + Spring Boot + SQLiteで実用的な単一テンプレートを作る。

実装担当はChatGPT 5.6 Lunaを想定する。各Phaseを上から順に実施し、検証が成功するまで次へ進まない。Java 8対応は設定値ではなく、実際のJava 8 JVMでbuild、test、起動した結果で判定する。

今回実装するのはJava 8 + SQLiteの1構成だけとする。将来のPostgreSQL版を見越し、DB固有実装はMyBatis mapper・migration・DataSource設定へ閉じ込めるが、DB切替機能やPostgreSQL実装は今回追加しない。

## 1. ゴール

fresh clone後に次の操作だけで開発を開始できる状態にする。

```bash
bun run bootstrap
bun run auth:create-admin -- --email admin@example.com --name "Admin User"
bun run dev

bun run verify
bun run verify:e2e
```

完了時に成立すること:

- Backendは実際のJava 8 JVMでbuild、test、起動できる。
- SQLite database fileが自動作成され、SQL migrationが安全に適用される。
- React + Vite frontendとSpring Boot APIを同じrepositoryで開発できる。
- production JARはfrontend buildを内包し、単一originで画面と`/api/*`を配信する。
- login、refresh token rotation、logout、`/me`、protected APIが動く。
- `hono-standard`のDesign System、Home、Login、Showcase、Protectedを利用できる。
- frontendは参照元から原則そのままコピーされ、Java API接続に必要な最小差分だけを持つ。
- security headers、CORS、CSRF、Cookie policy、production fail-closed validationを備える。
- unit、SQLite integration、contract、coverage、production build、Playwright E2Eが自動検証される。
- READMEと`LLM_CONTEXT.md`だけで利用方法と変更入口を判断できる。

## 2. 参照元とスコープ

### 2.1 参照元

実装開始時に参照元の状態を記録する。

```bash
git -C ../hono-standard rev-parse HEAD
git -C ../hono-standard status --short
```

確認時の参照元:

- HEAD: `83bf7d9`
- Backend/API: `../hono-standard/api/`
- Auth: `../hono-standard/api/modules/auth/`
- SQLite/migration: `../hono-standard/api/db/`, `../hono-standard/drizzle/`
- Frontend: `../hono-standard/web/`
- API schema: `../hono-standard/shared/schemas/`
- Verification: `../hono-standard/scripts/verify.ts`, `vitest.config.ts`, `tests/e2e/`
- DX/docs: `README.md`, `LLM_CONTEXT.md`, `.env.example`

`../hono-standard`は読み取り専用とする。編集、format、生成物削除、commit、branch切替を行わない。参照元に未コミット変更があっても上書きしない。

### 2.2 実装対象

- Java 8 + Spring Boot API
- SQLite file database
- SQL migration runner
- MyBatis mapperとrepository adapter
- password loginとCookie認証
- access JWTとone-time refresh token
- `hono-standard`からコピーしたReact SPA、Design System、Showcase
- Home、Login、Showcase、Protected画面
- `shared/schemas`のZod schemaとJava APIの契約検証
- OpenAPI document
- bootstrap、admin作成、development seed
- unit/integration/E2E/coverage/CI/docs

### 2.3 入れないもの

- SSG
- SSR
- PostgreSQL実装、DBのruntime切替、複数DataSource
- jOOQ、JPA/Hibernate
- FlywayやLiquibase
- Java側OpenAPI code generation
- OpenAPIからのfrontend型生成
- 複数Gradle module
- OAuth/OIDC、MFA、詳細RBAC、管理画面
- pgvector、RAG、wiki、agent機能
- Docker、Compose、container image
- Kubernetes、Terraform、cloud固有deployment
- Kotlin、Scala
- 将来構成のためだけのruntime switch、未使用実装、branch準備

### 2.4 Frontend copy contract

frontendは再実装ではなく、`../hono-standard`の追跡済みsourceを基準commitからコピーする。コピー元とコピー後の対応表を`docs/frontend-port.md`へ残す。

実装開始時に次を記録する。

- source commit: `git -C ../hono-standard rev-parse HEAD`
- source dirty state: `git -C ../hono-standard status --short`
- copy対象file list: `git -C ../hono-standard ls-files web shared/schemas`
- copy時点のfrontend dependency versionとroute list

`node_modules`、`dist*`、coverage、temporary fileはコピーしない。参照元のdirty fileは追跡済みHEAD版と作業tree版を区別し、今回の対象fileにdirty差分がある場合は勝手に採用せず`docs/frontend-port.md`へ明記する。コピー完了後はsource/target hash一覧を一度保存し、許可差分をfile単位で説明する。通常の`verify`はsibling repositoryへ依存させない。

そのままコピーする範囲:

- `web/index.html`
- `web/src/App.tsx`, `entry-client.tsx`, `router.tsx`
- `web/src/auth-context.tsx`, `domains/auth/`
- `web/src/routes/`のHome、Login、Showcase、Protected、Root
- `web/src/views/`のHome、Login、Showcase、Protected
- `web/src/styles.css`
- `web/src/showcase-settings-context.tsx`
- `web/src/showcase-table-search.ts`
- `web/src/components/`
- `shared/schemas/`
- frontend test、Playwright test、frontend用設定のうちSPAで必要なもの
- React、TanStack、lucide-react、react-hook-form、Zod、Tailwind、Vite、Vitest、Biomeの参照元と同じ固定version

コピーしない範囲:

- `web/src/entry-server.tsx`
- SSG/SSR build script、route manifest生成、server rendering用設定
- Hono server、Drizzle、Bun backend、Hono Vite plugin
- SSG/SSR専用testと生成物

frontendで変更を許す箇所は次に限定する。

1. `web/src/api.ts`から`hc<AppType>`を外し、同じ利用側interfaceを保つ小さな`fetch` adapterへ置換する。
2. Java APIのCookie/CSRF/error JSONへ合わせる。
3. alias、Vite proxy、build出力先をJava repository構成へ合わせる。
4. Hono固有型importをcopied Zod schemaまたは局所的なTypeScript typeへ置換する。
5. SSG/SSR entryとscriptへの参照を削除する。

Design token、CSS class、layout、Showcaseのtheme切替、table、検索状態、route構成を理由なく変更しない。API差し替えに不要なUI refactorも行わない。

## 3. 技術構成

必要最小限の構成に固定する。

| 項目 | 採用 | 理由・制約 |
| --- | --- | --- |
| JDK | Amazon Corretto 8 | build/test/runtimeの全てを実JDK 8で検証する |
| Build | Gradle 8.14.5 / Groovy DSL | Java 8でGradleを実行できる8.14系列をwrapper固定 |
| Backend | Spring Boot 2.7.18 | Java 8対応の最終OSS Spring Boot 2.x |
| Web | Spring MVC | Servlet APIは`javax.*`を使う |
| Security | Spring Security 5.7系 | Boot BOM管理版を使用 |
| DB access | MyBatis Spring Boot Starter 2.3.2 | Spring Boot 2.7 / Java 8対応が公式表とreleaseで確認できる |
| SQLite driver | `org.xerial:sqlite-jdbc:3.53.1.0` | current driver。base classfile 52とmanifest Java 8を確認済み |
| Migration | 独自の小さなSQL runner | `hono-standard`と同様にversioned SQLを順番に適用 |
| Access token | Spring Security OAuth2 JOSE / Nimbus | 独自JWT parserを作らない |
| Frontend | React 19 + Vite 8 + TanStack Router/Query/Table | `hono-standard`のfrontend依存とUIをそのままコピー |
| Frontend runtime | Bun 1.3.14 | install、dev、scripts、frontend testに限定 |
| API contract | copied Zod schema + OpenAPI 3.0.3 + fixture test | Hono型依存を外しつつfrontend schemaを維持 |
| Backend test | JUnit 5 + temporary SQLite file | 実SQLite/MyBatis mapperを検証 |
| E2E | Playwright Chromium | packaged JARとisolated SQLiteで検証 |

一次情報:

- Spring Boot 2.7.18 requirements: https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/
- Spring Boot 2.x OSS support終了: https://spring.io/blog/2023/11/23/spring-boot-2-7-18-available-now/
- Gradle Java compatibility: https://docs.gradle.org/current/userguide/compatibility.html
- Corretto support calendar: https://aws.amazon.com/corretto/faqs/
- Xerial SQLite JDBC: https://github.com/xerial/sqlite-jdbc
- MyBatis Spring Boot compatibility: https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/
- MyBatis Spring Boot 2.3.2 release: https://github.com/mybatis/spring-boot-starter/releases/tag/mybatis-spring-boot-2.3.2
- MyBatis configuration/database vendor support: https://mybatis.org/mybatis-3/configuration.html

dependency versionはdynamic range、`latest.release`、`+`を使わず完全固定する。Spring Boot BOM管理対象はBOMに従う。overrideが必要な場合は理由とJava 8実行結果を`docs/architecture.md`へ残す。

## 4. `hono-standard`との対応

| 必要な価値 | Java 8版での実現方法 | 完了証拠 |
| --- | --- | --- |
| one-command bootstrap | `.env`作成、Bun install、directory作成、SQLite migration | fresh実行と再実行が成功 |
| SQLite file DB | Xerial JDBC + `data/sqlite.db` | file作成、再open、永続化test |
| versioned migration | `db/migration/*.sql` + migration table | 初回適用、再実行skip、rollback test |
| MyBatis SQLite access | mapper interface + XML + repository adapter | 実file DBでCRUD/transaction test |
| API composition | Spring MVC controller + explicit service/repository | context/MockMvc test |
| frontend移植 | `web/`と`shared/schemas/`を基準commitからコピー | copy manifestと意図した差分だけが残る |
| Design System / Showcase | CSS、settings context、table/search、`/showcase`を維持 | visual smoke、route/unit/E2E test |
| frontend/backend契約 | copied Zod schema + OpenAPI + exact JSON fixture test | schema test、MockMvc fixture test |
| login | `POST /api/auth/login` | success、validation、曖昧な401 |
| access/refresh cookie | httpOnly access/refresh Cookie | Set-Cookie属性test |
| refresh rotation | hash保存したone-time tokenをatomic consume | success、期限切れ、再利用、競合test |
| logout revoke | refresh token削除 + Cookie clear | DB/HTTP test |
| `/me` | active userをDBで再確認 | inactive/delete後401 |
| protected sample | `/protected` + `/api/protected/profile` | frontendとserverの両方をE2E |
| Bearer fallback | `Authorization: Bearer`も許可 | filter test |
| 401 refresh retry | frontend fetch wrapperで一度だけrefresh | unit test + E2E |
| safe redirect | same-origin absolute pathだけ許可 | external redirect拒否test |
| security headers | Spring Security +明示CSP | MockMvc/curl assertion |
| CORS | origin allowlist + credentials | allowed/disallowed/preflight test |
| CSRF | CSRF Cookie + request header | tokenなし403、tokenあり成功 |
| production fail closed | secret/Cookie/origin設定を起動時検証 | context起動失敗test |
| admin CLI | interactive password / `--password-stdin` | CLI contract test |
| dev seed | development/testだけ許可 | production拒否test |
| single-origin production | JARがReact buildを静的配信 | packaged JAR E2E |
| verify gate | backend/frontend/contract/coverage/build | `bun run verify` |
| E2E gate | isolated DB + packaged JAR + Chromium | `bun run verify:e2e` |
| 80% coverage | backend application/auth/db対象 | JaCoCo verification |

### 4.1 Command contract

root `package.json`に次を定義し、README、CI、E2Eも同じ入口を使う。

| Command | Purpose |
| --- | --- |
| `bun run bootstrap` | `.env`、dependency、database directory、migrationを準備 |
| `bun run dev` | Spring BootとViteを同時起動 |
| `bun run start` | Spring Boot backendだけを起動 |
| `bun run db:migrate` | GradleのSQLite migration taskを実行 |
| `bun run auth:create-admin -- ...` | admin作成CLIへ引数とstdinを渡す |
| `bun run seed:dev` | development demo adminを冪等作成 |
| `bun run contract:check` | Zod/OpenAPI/Java response fixtureの整合を検証 |
| `bun run typecheck` | frontend TypeScript check |
| `bun run lint` | frontend lint |
| `bun run format` | frontend format write |
| `bun run format:check` | frontend format check |
| `bun run test` | frontend unit test |
| `bun run test:coverage` | frontend coverage |
| `bun run build:web` | React SPA production build |
| `bun run build` | frontendを内包したbootJarを作成 |
| `bun run smoke:start` | 実JDK 8でJAR起動、health確認、停止を自動実行 |
| `bun run verify` | repository全体の通常gate |
| `bun run verify:e2e` | packaged JARのPlaywright E2E |

Gradle側には少なくとも`dbMigrate`, `authCreateAdmin`, `seedDev`, `verifyJava8Bytecode`, `check`, `jacocoTestCoverageVerification`, `bootJar`を用意する。Bun scriptは薄いorchestratorとし、Javaの業務ロジックをTypeScript側に重複実装しない。

## 5. 目標repository構成

```text
.
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/wrapper/
├── package.json
├── bun.lock
├── .env.example
├── .gitignore
├── api-contract/
│   ├── openapi.yaml
│   └── fixtures/
├── db/
│   └── migration/
│       └── 0001_auth.sql
├── shared/
│   └── schemas/
├── src/
│   ├── main/
│   │   ├── java/com/example/javastandard/
│   │   │   ├── JavaStandardApplication.java
│   │   │   ├── app/
│   │   │   ├── auth/
│   │   │   ├── config/
│   │   │   ├── db/
│   │   │   │   ├── mapper/
│   │   │   │   ├── model/
│   │   │   │   └── repository/
│   │   │   └── web/
│   │   └── resources/
│   │       └── mybatis/sqlite/
│   └── test/
│       └── java/com/example/javastandard/
├── web/
│   ├── index.html
│   └── src/
├── scripts/
│   ├── bootstrap.ts
│   ├── dev.ts
│   ├── e2e-server.ts
│   ├── verify.ts
│   └── verify-java8-bytecode.ts
├── tests/e2e/
├── docs/
│   ├── architecture.md
│   ├── frontend-port.md
│   └── security.md
├── README.md
└── LLM_CONTEXT.md
```

単一Gradle projectとし、package境界だけで責務を分ける。再利用予定のない抽象moduleを作らない。

責務:

- `app/`: use case、transaction boundary、application error
- `auth/`: password、token、Cookie、authentication filter
- `config/`: environment、Spring Security、CORS、headers
- `db/mapper/`: MyBatis mapper interface。SQL本体は`resources/mybatis/sqlite/*.xml`
- `db/repository/`: application向けrepository adapter。MyBatis型を外へ漏らさない
- `db/model/`: mapper用row modelと明示的type変換
- `db/`: SQLite DataSource設定とmigration runner
- `web/`: controller、DTO、exception handler、SPA fallback

controllerからmapperやSQLを直接呼ばない。業務serviceはrepository interfaceに依存し、repository adapterがmapperを呼ぶ。migration SQL以外のapplication queryはMyBatis XMLへ集約する。Spring Security filterにlogin/refreshの業務ロジックを置かない。

### 5.1 将来のPostgreSQL版に対する境界

今回のSQLite版で保持する境界は、application serviceとrepository interface、Java DTO/API、frontend schemaだけとする。SQLite固有のSQL、PRAGMA、migration、generated-key取得、timestamp/boolean変換は`db`実装と`mybatis/sqlite`へ閉じる。

PostgreSQL版は将来、DataSource設定・migration・mapper XML・DB integration testを置換して作る。今回`databaseIdProvider`、DB自動判定、PostgreSQL dependency、空のPostgreSQL directoryは追加しない。MyBatisがJDBC DataSourceとvendor別statementを扱えることは採用根拠だが、runtime multi-vendor対応とはしない。

## 6. SQLite設計

### 6.1 Database path

- `DATABASE_URL`はURLではなくSQLite file pathとして扱う。
- local defaultは`data/sqlite.db`。
- production defaultも明示設定がなければ`data/sqlite.db`。
- 相対pathはprocess working directory基準でabsolute化する。
- parent directoryがなければmigration前に作成する。
- productionでは`postgres://`、`jdbc:*`、`file:`など別形式を拒否する。
- `:memory:`はtest profileだけ許可する。
- `.db`, `.sqlite`, `.sqlite3`, `data/`をcommitしない。

### 6.2 Connection settings

Xerial `SQLiteConfig`で全connectionに以下を適用する。

- `foreign_keys=ON`
- `journal_mode=WAL`
- `busy_timeout=5000`
- transactionは短く保つ

`:memory:`ではWALが成立しないため、test profileのin-memory testではjournal mode assertionを行わない。migration、transaction、再open、永続化の主要testは必ずtemporary file databaseで行う。

SQLiteは同時writerが1つであることを前提にする。refresh rotationなど競合があるwriteはtransactionとatomic conditionで保護する。connection数を無制限に増やさない。実装時に1〜4の範囲でintegration testを行い、最小の安定値を固定する。

### 6.3 Schema

`users`:

- `id text primary key`
- `email text not null`
- `normalized_email text not null unique`
- `password_hash text not null`
- `display_name text not null`
- `role text not null default 'member' check (role in ('admin','member'))`
- `is_active integer not null default 1 check (is_active in (0,1))`
- `last_login_at integer null`
- `created_at integer not null`
- `updated_at integer not null`

`refresh_tokens`:

- `id text primary key`
- `token_hash text not null unique`
- `user_id text not null references users(id) on delete cascade`
- `expires_at integer not null`
- `created_at integer not null`
- index: `user_id`, `expires_at`

timestampはUnix epoch millisecondsで統一する。emailはtrim後に`Locale.ROOT`でlowercaseした`normalized_email`で一意性を保証する。

### 6.4 Migration runner

- migration sourceは`db/migration/NNNN_name.sql`。
- filenameをlexicographic順に適用する。
- `java_standard_schema_migrations(filename primary key, applied_at integer not null)`で適用済みを管理する。
- migration file単位でtransactionを開始し、失敗時はrollbackする。
- application起動前、`db:migrate` CLI、test setupで同じrunnerを使う。
- 既存migration fileを変更しない。変更は新しい連番fileで行う。
- 空file、重複filename、読めないresourceは明示的に失敗する。
- SQL statementを単純な`;` splitで壊さない。SQLite JDBCのscript実行方法を検証し、trigger/commentを扱える最小parserまたはstatement readerを使う。

### 6.5 MyBatis実装規約

- `mybatis-spring-boot-starter:2.3.2`を固定し、Starterが既存`DataSource`から`SqlSessionFactory`と`SqlSessionTemplate`を構成する標準経路を使う。
- mapperはinterface + XMLとし、複雑なSQLをannotation文字列へ埋め込まない。
- `mapper-locations=classpath*:mybatis/sqlite/*.xml`を明示する。
- `mapUnderscoreToCamelCase=true`に依存しすぎず、重要なresultは`resultMap`で列とpropertyを明示する。
- `localCacheScope=STATEMENT`とし、認証状態の古いreadがsession cacheに残る余地を減らす。
- transactionはSpring `@Transactional`でservice/use caseに置き、refresh token消費は条件付き`DELETE`または`UPDATE`のaffected rowsで1回性を保証する。
- SQLite booleanは`INTEGER 0/1`、timestampはepoch millisとしてtype handlerまたはrepository mapperで明示変換する。
- migration runnerだけはschema適用のためJDBCを直接使ってよい。application CRUDで`JdbcTemplate`とMyBatisを混在させない。
- mapper XMLの起動時parse test、statement id重複test、全mapper integration testを用意する。

## 7. API・認証・セキュリティ契約

### 7.1 API

| Method | Path | Access | Response |
| --- | --- | --- | --- |
| `GET` | `/api/health` | public | `{ "status": "ok" }` |
| `GET` | `/api/csrf` | public | CSRF token + readable Cookie |
| `POST` | `/api/auth/login` | public + CSRF | `{ user }` + auth Cookie |
| `POST` | `/api/auth/refresh` | refresh Cookie + CSRF | `{ user }` + rotated Cookie |
| `POST` | `/api/auth/logout` | session optional + CSRF | `{ "ok": true }` + Cookie clear |
| `GET` | `/api/auth/me` | access token | `{ user }` |
| `GET` | `/api/protected/profile` | access token | `{ profile: { email, role } }` |

共通error body:

```json
{
  "message": "Unauthorized",
  "code": "AUTH_UNAUTHORIZED",
  "fieldErrors": {}
}
```

- productionの500では内部exception messageを返さない。
- login失敗はuser不存在、password不一致、inactiveを区別しない。
- validation errorはfield単位で返すが、機密情報を含めない。
- unknown `/api/*` はJSON 404。SPA fallbackへ流さない。

### 7.2 Passwordとtoken

- passwordはSpring Security `DelegatingPasswordEncoder` + bcryptを使う。
- admin passwordは12文字以上。login requestは列挙耐性のため空でないことだけを検証し、認証失敗を同じ401にする。
- access tokenはHS256 JWT、default 15分。
- access JWTは`sub`, email, role, type, iat, exp, jtiを持つ。
- refresh tokenは`SecureRandom`で32 bytes生成しBase64URL encodeするopaque token。
- DBにはrefresh tokenのSHA-256 hashだけを保存する。
- refresh時は未使用tokenをtransaction内で1回だけconsumeし、新tokenを発行する。
- 同じrefresh tokenの並行requestは1件だけ成功し、他は401になる。
- logoutは該当refresh tokenを削除する。tokenなしでもidempotentに成功する。
- password、JWT、refresh token、secretをlogしない。

### 7.3 Cookie・CSRF・CORS

- access Cookie: `HttpOnly`, configurable `Secure`, configurable `SameSite`, `Path=/`。
- refresh Cookie: `HttpOnly`, configurable `Secure`, configurable `SameSite`, `Path=/api/auth`。
- CSRF Cookie: JavaScriptから読める`XSRF-TOKEN`。
- state-changing requestは`X-XSRF-TOKEN` headerを送る。
- `SameSite=None`かつ`Secure=false`は起動拒否。
- productionでdevelopment JWT secret、短いsecret、wildcard credential CORSを拒否。
- allowed originは完全一致。substring/suffix一致にしない。
- CSPは最低限`default-src 'self'`, `object-src 'none'`, `frame-ancestors 'self'`, `base-uri 'self'`を含む。

### 7.4 Environment

`.env.example`、Spring `@ConfigurationProperties`、READMEの名称とdefaultを一致させる。

| Variable | Local default | Production rule |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `development` | `production`を明示 |
| `HOST` | `127.0.0.1` | 公開環境では明示設定 |
| `PORT` | `8080` | 1〜65535 |
| `DATABASE_URL` | `data/sqlite.db` | 書込み可能な永続file pathを明示 |
| `JWT_SECRET` | development専用値 | 32 bytes以上のrandom secret必須 |
| `JWT_ACCESS_TTL` | `15m` | 正のduration |
| `JWT_REFRESH_TTL` | `30d` | accessより長い正のduration |
| `APP_URL` | `http://localhost:5173` | public originを明示 |
| `CORS_ORIGINS` | `http://localhost:5173` | comma区切り完全一致、`*`禁止 |
| `AUTH_COOKIE_SECURE` | `false` | HTTPS公開では`true` |
| `AUTH_COOKIE_SAME_SITE` | `lax` | `none`はSecure必須 |
| `SECURITY_HEADERS_MODE` | `auto` | `auto`/`http`/`https` |

空文字、不正boolean、未知enum、不正durationを黙ってdefaultへ変換せず起動時に拒否する。secretは起動logやerror responseへ出さない。

## 8. Frontend・production配信

### 8.1 画面

- `/`: public Home
- `/login`: public/session-aware Login
- `/showcase`: public Design System / component Showcase
- `/protected`: login-required sample

画面構成、Design token、Showcaseのtheme設定とtable/search behaviorはコピー元を維持する。Java API接続と無関係な見た目の変更をしない。

### 8.2 Frontend API client

- `shared/schemas`のZod schemaをfrontend request/response validationに継続利用する。
- request処理は小さな手書きfetch wrapperに集約する。
- 全requestで`credentials: include`。
- POST前にCSRF tokenを取得しheaderへ設定。
- auth以外のrequestが401ならrefreshを一度だけ試し、成功時だけ元requestを再実行。
- `/api/auth/me`の401はrefreshせずlogged-outとして扱う。
- refresh失敗時はauth stateをclearし、refresh loopを起こさない。
- login redirectは`/`から始まるsame-origin pathだけ許可する。

### 8.3 Buildと配信

- Viteは通常のReact SPAだけをbuildする。
- frontend buildをGradle `processResources`時に`build/generated-resources/static`へcopyする。
- generated frontend outputをcommitしない。
- Spring Boot JARは静的assetと`index.html`を配信する。
- non-API frontend routeへの直接GETは`index.html`へforwardする。
- `/api/**`はSPA fallback対象外。
- runtimeにBun/Nodeを必要としない。

## 9. Luna向け実行ルール

各Phaseで必ず行う:

1. 対象fileと参照元を先に読む。
2. 実装前の該当verification結果を記録する。
3. Phase外の機能を追加しない。
4. targeted testを通す。
5. Phase完了commandを通す。
6. `git diff --check`と`git status --short`を確認する。
7. 失敗、skip、暫定回避を成功として報告しない。

禁止事項:

- Java 9以降のAPI、record、`var`、text block、module-infoを使わない。
- `sourceCompatibility=1.8`だけでJava 8対応と判断しない。
- SQLite integration testをmock repositoryだけで代替しない。
- CSRFを無効化しない。
- production secretをrepositoryへ置かない。
- copied schemaをJava実装へ合わせるためだけに一方的に緩めない。
- frontendを全面再設計しない。
- 参照元repositoryを変更しない。
- 要求されていない抽象化や追加機能を入れない。

## 10. Phase別実装計画

### Phase 0: Java 8 + SQLite互換性スパイク

目的: tool、plugin、SQLite driverが実際のJava 8で動くことを最初に証明する。

作業:

1. 参照元のbranch、commit、status、commands、routesを記録。
2. Gradle wrapper 8.14.5を作成。
3. 最小Spring Boot 2.7.18 applicationを作成。
4. Xerial SQLite JDBC 3.53.1.0をresolve。
5. MyBatis Spring Boot Starter 2.3.2をresolveし、mapper XML経由でtemp SQLite fileへtable作成、insert、select、close、再openを実行。
6. formatter/linter/JaCoCoを含むGradle pluginをJava 8でload。
7. runtime/testRuntime JARのbase classを走査し、classfile major 52超を検出するtaskを作る。Multi-Release JARはmanifestと`META-INF/versions/*`を区別する。
8. `JAVA8_HOME`必須のwrapper scriptを作り、実行JVMが`1.8`でなければ検証を失敗させる。

検証:

```bash
JAVA_HOME="$JAVA8_HOME" PATH="$JAVA8_HOME/bin:$PATH" java -version
JAVA_HOME="$JAVA8_HOME" PATH="$JAVA8_HOME/bin:$PATH" ./gradlew --no-daemon clean test bootJar
JAVA_HOME="$JAVA8_HOME" PATH="$JAVA8_HOME/bin:$PATH" ./gradlew --no-daemon verifyJava8Bytecode
```

完了条件:

- Gradle、全plugin、compile、test、bootJarがJava 8で成功。
- temp SQLite fileの再open後もdataを取得できる。
- runtime base classにmajor 53以上がない。
- 非互換dependencyはこのPhaseで修正し、理由を記録する。

### Phase 1: 単一project scaffoldとfrontend baseline copy

目的: backendの最小境界を作り、参照元frontendを変更前baselineとして取り込む。

作業:

- 単一Spring Boot Gradle projectを作成。
- packageをSection 5の責務に分ける。
- Java 8 source/targetと実JDK 8 testを設定。
- dependency lockingを有効化。
- Section 2.4のcopy contractに従って`web/`、`shared/schemas/`、frontend testと必要設定をコピー。
- root `package.json`は参照元frontend dependency versionを維持し、Hono/Drizzle/backend dependencyとSSG/SSR scriptだけを除く。
- `.gitignore`へ`.env`, `data/`, DB、build、coverage、Playwright outputを追加。
- `docs/frontend-port.md`へ参照commit、コピー対象、除外対象、意図した差分を記録。
- copied route `/`, `/login`, `/showcase`, `/protected`を維持する。
- CI骨格を作るが、完成していないgateを成功扱いするplaceholderは置かない。

検証:

```bash
./gradlew clean test bootJar
bun install --frozen-lockfile
bun run typecheck
bun run build:web
```

完了条件:

- JAR起動とfrontend buildが成功。
- Home/Login/Showcase/ProtectedとDesign System sourceが存在する。
- SSG/SSR/Hono server/Drizzle dependencyがない。
- frontend差分がSection 2.4の許可範囲だけである。

### Phase 2: SQLite・migration・repository

目的: `hono-standard`に近いSQLite runtimeを完成させる。

作業:

- `SQLiteConfig`とSpring `DataSource`、MyBatis Starter設定を実装。
- path validation、parent directory作成、test-only memory DB制約を実装。
- `0001_auth.sql`とmigration runnerを実装。
- application起動前にmigrationを適用。
- `db:migrate` commandでも同じrunnerを呼ぶ。
- user/refresh tokenのmapper interface、SQLite用mapper XML、repository adapterを実装。
- row mappingとboolean/timestamp変換を`resultMap`、type handler、repository adapter内に閉じる。
- refresh tokenのatomic consume SQLをmapper XMLに実装する。
- temp directoryのfile DBを使うintegration testを作る。

必須test:

- path/default/relative/absolute/invalid URL
- parent directory作成
- foreign key有効
- WAL/busy timeout設定
- migration初回/再実行/失敗rollback/順序
- user insert/find/update/duplicate email/inactive
- refresh token insert/find/delete/expiry/cascade
- mapper XML parse、全statement resolve、generated key
- concurrent refresh consumeでaffected rowsが1件だけ1になること
- process再open後の永続化

検証:

```bash
./gradlew test --tests '*db*'
./gradlew dbMigrate
./gradlew dbMigrate
```

完了条件:

- migration 2回目が変更なしで成功。
- database file再open後もschema/dataが残る。
- application SQLは`resources/mybatis/sqlite`、schema SQLは`db/migration`以外にない。
- application service/controllerはMyBatis型へ依存しない。

### Phase 3: Auth core・Spring Security・API

目的: 認証と保護APIを完成させる。

作業:

- password、JWT、opaque refresh token serviceを実装。
- login、refresh、logout、current user、admin作成use caseを実装。
- refresh consume/rotateをSQLite transactionでatomicにする。
- typed environment validationを実装。
- Spring Security filter chain、Cookie/Bearer authentication、CSRF、CORS、headersを実装。
- health/auth/protected controllerとglobal exception handlerを実装。
- copied Zod schemaを正としてJava DTOとOpenAPI documentを対応させる。
- Zod fixture、Java DTO serialization、OpenAPI exampleのexact contract testを作る。

必須test:

- password hash/verify/wrong/malformed
- access JWT generate/verify/expired/tampered/wrong type
- login success/unknown/wrong password/inactive
- refresh success/expired/reused/concurrent consume
- logout with/without token
- admin create/duplicate
- env default/override/invalid/production weak secret
- Cookie属性とclear path
- CSRFあり/なし
- allowed/disallowed CORS、preflight
- security headers/CSP
- protected API Cookie/Bearer/no token/inactive
- validation/error JSON/production 500 masking
- unknown API JSON 404

検証:

```bash
./gradlew test --tests '*auth*' --tests '*web*' --tests '*config*'
bun run contract:check
```

完了条件:

- Section 7.1の全endpointをMockMvcで確認。
- raw refresh tokenがDB/logに存在しない。
- concurrent refreshは1件だけ成功。

### Phase 4: React SPA統合

目的: copied Home/Login/Showcase/Protected UIを保ったままJava APIへ接続する。

作業:

- Phase 1でコピーしたapp shell、Design System、Home、Login、Showcase、Protectedをbaselineとして使用。
- `web/src/api.ts`のHono `hc<AppType>`だけを、利用側interfaceを維持する`fetch` adapterへ置換。
- Hono固有型をcopied Zod schemaと局所typeへ置換。
- SSG/SSR source、script、build entryは取り込まない。
- CSRF、credentials、401 refresh retry、unauthorized eventを実装。
- auth context、session-aware login、protected routeを実装。
- Vite dev serverの`/api` proxyをSpring Bootへ向ける。
- Gradle production buildへfrontend asset copyを組み込む。
- SPA fallbackとAPI 404境界を実装。
- copy後のUI差分を再確認し、API接続に無関係な差分を戻す。

frontend tests:

- `/me` 401はnullでrefreshしない
- protected request 401はrefreshを1回だけ実行
- refresh failureでsession clear
- CSRF取得/header送信
- safe redirect
- login form/error
- protected route access
- Showcase route、theme/settings、table search state
- copied Zod schemaによるresponse validation

検証:

```bash
bun run typecheck
bun run lint
bun run format:check
bun run test
bun run test:coverage
bun run build:web
./gradlew bootJar
```

完了条件:

- Home/Login/Showcase/Protectedが操作可能。
- JARからfrontendを配信できる。
- `/login`、`/showcase`、`/protected`直接GETが200。
- `/api/unknown`はHTMLではなくJSON 404。
- source/build outputにSSG/SSR entryがない。
- Design token、Showcase component、routeの意図しない欠落がない。

### Phase 5: bootstrap・CLI

目的: clone直後の導入体験とlocal/host JVM運用を完成させる。

作業:

- `bun run bootstrap`:
  - `.env`がなければ`.env.example`から作成
  - existing `.env`の利用者値を破壊しない
  - Bun dependencyをfrozen lockfileで導入
  - `data/`を作成
  - `./gradlew dbMigrate`を実行
  - 2回目も成功
- `bun run dev`でSpring BootとViteを起動し、一方の失敗時に両方を停止。
- `auth:create-admin`にinteractive passwordと`--password-stdin`を実装。
- `seed:dev`はdevelopment/testだけ許可。
- `bun run start`はpackaged JARまたは明示されたJARを実JDK 8で起動する。
- `bootstrap`、CLI、dev scriptはspaceを含むpathと既存`.env`でtestする。

検証:

```bash
rm -f .env
rm -rf data
bun run bootstrap
bun run bootstrap
printf '%s\n' 'password123456' | bun run auth:create-admin -- --email admin@example.com --name "Admin User" --password-stdin
JAVA_HOME="$JAVA8_HOME" PATH="$JAVA8_HOME/bin:$PATH" bun run smoke:start
```

完了条件:

- fresh bootstrapと再実行が成功。
- admin作成後login可能。
- productionでdev seed拒否。
- process再起動後も`data/sqlite.db`とsessionが期待どおり扱われる。
- Java 8以外でJava verification/startを実行した場合は明示的に失敗する。

### Phase 6: verify・E2E・CI・docs

目的: テンプレートの完了条件を自動化する。

作業:

- Playwrightは`tmp/e2e.sqlite`、fresh migration、seed admin、packaged JARを使う。
- E2E:
  1. Home/Login/Showcase表示
  2. Showcase theme切替、table/searchの代表操作
  3. `/protected`未ログイン表示
  4. redirect付きlogin
  5. protected APIがadminを確認
  6. access token期限切れ後refresh retry
  7. logout後protected API 401
  8. `/login`、`/showcase`、`/protected`直接navigation
- JaCoCo対象をapplication/auth/db/webにし、line/branch/instruction 80%以上。
- generated DTO、configuration entrypoint等の除外は理由を明記し、広い除外を禁止。
- `bun run verify`:
  - Java 8 bytecode verification
  - Java format/lint
  - backend unit/SQLite integration test
  - JaCoCo coverage
  - Zod/OpenAPI/Java fixture contract check
  - frontend typecheck/lint/format/test/coverage
  - production frontend + bootJar build
- `bun run verify:e2e`: isolated SQLite + packaged JAR + Playwright。
- CIをverify/e2eに分け、actionをcommit SHAでpin。
- Java 8非対応のsecurity scannerは固定versionの独立CI stepで実行。
- READMEにsetup、Java 8導入、env、commands、API、routes、SQLite backup、security、Java 8 OSS support riskを記載。
- `LLM_CONTEXT.md`にtask routing、package boundary、frontend copy policy、verification matrixを記載。

最終検証:

```bash
JAVA_HOME="$JAVA8_HOME" PATH="$JAVA8_HOME/bin:$PATH" bun run verify
JAVA_HOME="$JAVA8_HOME" PATH="$JAVA8_HOME/bin:$PATH" bun run verify:e2e
JAVA_HOME="$JAVA8_HOME" PATH="$JAVA8_HOME/bin:$PATH" bun run smoke:start
git diff --check
git status --short
```

完了条件:

- 全command成功。
- coverage threshold成功。
- localとCIのverify入口が同じ。
- `.env`、SQLite DB、coverage、Playwright、build outputが追跡されていない。
- README手順だけでfresh setupを再現できる。
- Section 4の全項目に自動testまたはsmoke evidenceがある。
- source、script、build task、dependencyに不要機能が残っていない。

## 11. 完了定義

次をすべて満たした場合だけ終了する。

1. Corretto 8上でbackend build/test/JAR起動が成功する。
2. `bun run verify`が成功する。
3. `bun run verify:e2e`がfresh SQLite fileで成功する。
4. migration初回/再実行/rollback/再openが成功する。
5. login、refresh rotation、logout、`/me`、protected API、inactive拒否が通る。
6. Home/Login/Showcase/Protectedとdirect navigationが通る。
7. copied Zod schema、Java DTO、OpenAPI exampleのcontract checkが通る。
8. CORS、CSRF、Cookie、security headers、production fail-closed testが通る。
9. backend coverage 80%以上。
10. bootstrapがfresh実行と再実行の両方で成功する。
11. production JARがJava 8で起動し、process再起動後もSQLite fileを利用できる。
12. README記載手順だけで第三者が起動・検証できる。
13. Design SystemとShowcaseが参照元相当で残り、frontend差分がAPI adapter等の許可範囲に収まる。
14. MyBatis mapperが実SQLite fileで検証され、controller/serviceへDB固有実装が漏れていない。
15. SSG、SSR、PostgreSQL実装、DB runtime切替、不要なORMやmigration frameworkが含まれていない。

未完了testのskip、既知failure、手動確認待ち、Java 17でのみ成功するbuildを完了としてはならない。
