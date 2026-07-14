# Java 25 SQLite Template Implementation Plan

## 0. 位置づけ

この計画は、masterのJava 8版を維持したまま、variant/java25ブランチでJava 25を実行基盤とするSQLiteテンプレートを作るためのものです。実装担当はChatGPT-5.6 Lunaを想定します。

- DBはSQLiteのみ。PostgreSQL、runtime DB切替、複数DataSourceは対象外。
- web、Design System、Showcase、shared/schemas、OpenAPI、既存API契約は原則維持。
- SSG、SSR、Docker、Kotlin、Scala、OAuth/OIDC、MFA、管理画面は入れない。
- Java 25のpreview featureは使わず、標準化済み機能だけを採用する。

Java 25版はJava 8版の単純なsourceCompatibility変更ではなく、Spring Boot 4/Jakarta/MyBatis 4へ世代更新した別バリアントとする。一方、Java 25移行と無関係なUI再設計や機能追加は行わない。

## 1. 完了条件

1. variant/java25がmasterから分岐している。
2. Java 25 JVM上でGradle、compile、test、bootJar、実行が成功する。
3. Spring Boot 4系、Spring Framework 7系、Spring Security 7系、MyBatis Spring Boot Starter 4系へ移行できている。
4. javax.servlet、javax.validation、javax.annotationをjakarta.*へ移行できている。JDK標準のjavax.sql、javax.cryptoは無意味に置き換えない。
5. SQLite migration/MyBatis CRUD、login、refresh rotation、logout、/me、protected API、CSRF、CORS、Cookie policyがJava 8版と同じ契約で動く。
6. コピー済みfrontend、Design System、Showcase、OpenAPI、Zod fixtureが意図しない変更なしに動く。
7. packaged JARがfrontendを内包し、single-originでSPAと/api/*を配信する。
8. backendのJaCoCo instruction/line coverageがともに80%以上で、除外理由が文書化されている。
9. Java 25版の通常gateとpackaged JARのE2E gateが成功する。
10. git diff master...variant/java25でJava 8版への逆流、不要な依存、SSG/SSR/Dockerがない。

## 2. 採用構成と根拠

| 項目 | 採用方針 |
| --- | --- |
| JDK | Java 25 LTS。ローカルはJAVA25_HOME、CIはTemurin 25等を固定 |
| Build | Gradle 9.1.0以上をwrapperで固定。Java 25でGradleを実行するため |
| Backend | Spring Boot 4.0.6を第一候補として完全固定。4.1が実装開始時点で安定版なら、リリースノートと主要依存の対応を確認した場合だけ置換可 |
| Web/Security | Spring MVC、Servlet 6.1、Spring Security 7、Jakarta Validation |
| DB access | org.mybatis.spring.boot:mybatis-spring-boot-starter:4.0.0 |
| SQLite | Xerial SQLite JDBCの実装時安定版を完全固定。既存migrationとSQLを維持 |
| Migration | 既存のversioned SQL runnerを継続。Flyway/Liquibaseは追加しない |
| JWT | Spring Security OAuth2 JOSE/Nimbus。独自parserは作らない |
| Coverage | JaCoCo 0.8.15以上。Java 25 class fileを公式対応する版を使う |
| Frontend | 既存React/Vite/TanStack/Bun構成を維持 |

一次資料:

- [Gradle Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html)
- [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [MyBatis Spring Boot Starter compatibility](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)
- [Java SE 25 language changes](https://docs.oracle.com/en/java/javase/25/language/java-language-changes-summary.html)
- [JaCoCo change history](https://www.jacoco.org/jacoco/trunk/doc/changes.html)

### Java 25機能の利用方針

- request/response DTO、認証結果、immutable value objectはrecords化を優先する。
- switch expression、pattern matching、immutable collectionを、既存の意味を変えない範囲で使う。
- sealed interfaceは認証状態など状態集合が固定できる箇所だけ検討する。
- virtual threadは初期値を無条件に有効化しない。SQLiteのsingle-writer制約、Hikari設定、同時実行テストを通過した場合だけ任意設定として追加する。
- preview feature、Structured Concurrency、native image、Lombok、JPA/Hibernateは完了条件に含めない。

## 3. 維持するもの／見直すもの

### 維持するもの

- webのHome、Login、Protected、Showcase、Design System、styles、route構成
- shared/schemas、api-contract/openapi.yaml、api-contract/fixtures
- db/migration/0001_auth.sqlのデータモデルとAPIの意味
- same-origin fetch adapter、Vite proxy、JAR内frontend配信
- SSG/SSRなし、Dockerなしの構成

### 見直すもの

- build.gradle、Gradle wrapper、settings.gradle、gradle.properties
- src/main/javaのJakarta移行、Boot 4/Security 7差分、records化候補
- src/test/javaのJakarta/JUnit/MockMvc差分、coverage不足テスト
- application.yml、.env.example、scripts/env.ts、scripts/verify.ts、起動・E2E scripts
- README.md、LLM_CONTEXT.md、docs/architecture.md、docs/security.md

../hono-standardは読み取り専用とし、frontendを再コピーしない。検証時のruntime dependencyにもならない。

## 4. ブランチとベースライン

### Phase 0: ブランチ準備

作業開始時にcleanを確認し、masterからvariant/java25を作る。

    git status --short --branch
    git switch -c variant/java25
    git rev-parse master
    git rev-parse HEAD

分岐直後にJava 8版のbaselineを採取する。

    bun run typecheck
    bun run test
    bun run contract:check

baseline、参照元frontend commit、source dirty stateをdocs/frontend-port.mdまたは実装記録へ残す。このphaseでは計画書とブランチ準備以外を変更しない。

## 5. 実装phase

### Phase 1: Java 25ビルド基盤

1. Gradle wrapperを9.1.0以上へ更新する。
2. java.toolchain.languageVersion = JavaLanguageVersion.of(25)、JavaCompileのoptions.release = 25を設定する。
3. JAVA25_HOMEを検証するverifyJava25Runtimeを追加する。
4. classfile major version 69を検証するverifyJava25Bytecodeを追加する。
5. buildWeb、bootJar、dbMigrate、authCreateAdmin、seedDevがJava 25を使うようにする。

完了証拠:

    JAVA25_HOME=<JDK 25> ./gradlew --no-daemon verifyJava25Runtime compileJava verifyJava25Bytecode

### Phase 2: Spring Boot 4 / Jakarta / MyBatis 4

1. Spring Boot 4.0.6、BOM、MyBatis starter 4.0.0を完全固定する。4.1へ変更する場合は、変更理由と互換性検証結果を先に記録する。
2. javax.servlet.*、javax.validation.*、javax.annotation.*を対応するjakarta.*へ移行する。
3. javax.sql.DataSourceとjavax.crypto.*はJDK標準なので残す。
4. Security 7のSecurityFilterChain、CSRF、CORS、OAuth2 JOSE、exception handling差分をcompileだけでなくMockMvcで確認する。
5. Boot 4のJSON mapper/error responseを既存fixtureと同じフィールド、status、content typeへ固定する。

完了証拠:

- ./gradlew testでcontext load、login、refresh、logout、CSRF、protected APIが成功する。
- アプリケーションコードに不要なjavax.servlet/javax.validation/javax.annotationが残らない。

### Phase 3: Java 25らしいモデル整理

1. 外部JSON契約を変えず、AuthResult、AuthUser、AuthResponse、SessionUser、LoginRequest等を一つずつrecords化する。
2. Jackson、validation、MyBatis result mappingに問題が出る型は通常classのまま残し、内部value objectを優先する。
3. sealed typeやpattern matchingは、認証状態など分岐が固定できる箇所だけに使う。
4. Java 25機能利用を目的にした一括リファクタリングはしない。

### Phase 4: SQLite/MyBatisの実運用設定

1. connection initializationでforeign_keys=ON、busy_timeout、WALを明示し、integration testで確認する。
2. migrationのversion table、再実行skip、失敗時rollback、同時起動を実SQLiteで確認する。
3. MyBatis 4 mapper XML、transaction、CRUD、refresh rotation、admin seed競合を実DBで確認する。
4. virtual threadを試す場合はSQLite同時実行テストを先に通し、失敗時は無効のまま完了する。

### Phase 5: Frontend / contract / packaged JAR

1. frontendのAPI adapterは変更しないことを第一選択とし、JSON/error shape差分が出た場合だけ局所修正する。
2. contract:checkでOpenAPI、Zod、Java fixtureを検証する。
3. buildWebからbootJarへfrontendを内包する。
4. 一時SQLiteでJARを起動し、health、SPA route、login、refresh、protected、logout、CSRFをPlaywrightで検証する。

### Phase 6: Coverage / docs / cleanup

1. JaCoCo 0.8.15以上でinstruction/lineの両方に80% minimumを設定する。
2. CLIなどprocess boundaryを除外する場合は理由をdocs/architecture.mdへ記録する。カバレッジ目的だけの除外追加は禁止する。
3. Java 8版との差分、要求JDK、起動方法、virtual thread既定値、SQLite制約をREADME/LLM_CONTEXT/docsへ記載する。
4. SSG/SSR、Docker、PostgreSQL、Kotlin/Scala、Java 8専用scriptの残骸を削除する。

## 6. Command contract

Java 25版のroot commandはJava 8版と意味を揃え、runtimeだけを置き換える。

| Command | 目的 |
| --- | --- |
| bun run bootstrap | .env、Bun依存、SQLite directory、migration準備 |
| bun run dev | ViteとSpring Bootを同時起動 |
| bun run start | Java 25 backendだけを起動 |
| bun run db:migrate | Java 25のmigration task |
| bun run auth:create-admin -- ... | Java 25 CLIへ引数/stdinを渡す |
| bun run seed:dev | development seed |
| bun run contract:check | Zod/OpenAPI/Java fixture整合 |
| bun run test | frontend unit test |
| bun run test:coverage | frontend coverage |
| bun run build | frontend内包bootJar |
| bun run smoke:start | Java 25 JAR起動とhealth確認 |
| bun run verify | frontend + Java 25通常gate |
| bun run verify:e2e | packaged JARのPlaywright E2E |

GradleにはverifyJava25Runtime、verifyJava25Bytecode、dbMigrate、authCreateAdmin、seedDev、jacocoTestReport、jacocoTestCoverageVerification、check、bootJarを用意する。

## 7. 検証ゲート

### Gate A: 基盤

- java -versionが25、Gradleが9.1.0以上、classfile majorが69
- compileJava、compileTestJava

失敗時はJDK/Gradle/toolchainだけを修正する。

### Gate B: backend

    JAVA25_HOME=<JDK 25> ./gradlew --no-daemon clean test jacocoTestReport jacocoTestCoverageVerification

context load、実SQLite migration、mapper CRUD、auth lifecycle、security、coverage 80%以上を確認する。

### Gate C: contract/frontend

    bun run typecheck
    bun run lint
    bun run format:check
    bun run contract:check
    bun run test
    bun run test:coverage
    bun run build:web

Java responseをfixtureと比較し、UIリファクタリングで契約差分を隠さない。

### Gate D: packaged runtime

    JAVA25_HOME=<JDK 25> bun run verify
    JAVA25_HOME=<JDK 25> bun run verify:e2e

一時SQLiteでJARのfrontend配信、health、login、refresh rotation、logout、token replay拒否、inactive user拒否、CSRF/CORS拒否を確認する。

### Gate E: 差分レビュー

    git diff --check master...variant/java25
    git diff --stat master...variant/java25
    git status --short

Java 8版、../hono-standard、不要なSaaS/DB/コンテナ機能が変更されていないことを確認する。

## 8. コミット分割

1. docs: plan Java 25 SQLite variant
2. build: move template to Java 25 toolchain
3. build: upgrade Spring Boot and MyBatis for Java 25
4. refactor: modernize Java domain types
5. test: verify Java 25 SQLite runtime
6. docs: document Java 25 variant

各コミット後に該当Gateを実行し、最終コミット前に全Gateとclean worktreeを確認する。

## 9. 明示的にやらないこと

- masterへ直接変更しない。
- ../hono-standardを編集しない、runtime dependencyにしない。
- frontendの見た目、route、Design System、Showcaseを移行のついでに変更しない。
- SSG/SSR、Docker、PostgreSQL、Kotlin、Scala、JPA/Hibernateを追加しない。
- virtual thread、preview feature、native imageを完了条件にしない。
- dynamic dependencyを使わない。
- compileだけで完了扱いにせず、実SQLite、packaged JAR、E2E、coverageまで検証する。
