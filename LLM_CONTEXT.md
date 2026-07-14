# LLM Context

## Objective

This repository is the Java 25 + SQLite variant of the standard template. Keep the copied `web/`, `shared/`, Design System, and Showcase behavior aligned with `../hono-standard` unless a backend integration requires a small adapter change.

## Runtime boundary

- Java 25 is the required runtime; compile and runtime checks use `JAVA25_HOME`.
- Spring Boot 4.0.6 owns HTTP and security wiring.
- MyBatis Spring Boot Starter 4.0.0 owns SQL mapping; SQLite SQL is under `src/main/resources/mybatis/sqlite`.
- SQLite is the only active database. Do not add PostgreSQL or Docker to the default path.
- Vite serves the frontend in development; the packaged JAR serves `dist-web` as classpath static resources.
- Do not reintroduce SSG, SSR, or a second server-rendering entrypoint.

## Change rules

1. Preserve the API contract in `api-contract/openapi.yaml` and the shared schemas.
2. Keep auth cookies, CSRF checks, refresh-token rotation, and migration idempotency covered by tests.
3. Prefer standardized Java 25 language/API features when they preserve the existing contract; do not use preview features.
4. If a future PostgreSQL variant is needed, add a profile-specific mapper/migration implementation behind the repository boundary; do not alter SQLite defaults.

## Verification

Run `bun run verify` with `JAVA25_HOME` set, then `bun run test:e2e`. Inspect `git diff --check` and confirm no Hono backend, Drizzle, SSG, SSR, or Docker dependency was introduced.
