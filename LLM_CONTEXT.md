# LLM Context

## Objective

This repository is the Java 8 + SQLite variant of the standard template. Keep the copied `web/`, `shared/`, Design System, and Showcase behavior aligned with `../hono-standard` unless a backend integration requires a small adapter change.

## Runtime boundary

- Java 8 is the compatibility floor; compile and runtime checks use `JAVA8_HOME`.
- Spring Boot 2.7.18 owns HTTP and security wiring.
- MyBatis 2.3.2 owns SQL mapping; SQLite SQL is under `src/main/resources/mybatis/sqlite`.
- SQLite is the only active database. Do not add PostgreSQL or Docker to the default path.
- Vite serves the frontend in development; the packaged JAR serves `dist-web` as classpath static resources.
- Do not reintroduce SSG, SSR, or a second server-rendering entrypoint.

## Change rules

1. Preserve the API contract in `api-contract/openapi.yaml` and the shared schemas.
2. Keep auth cookies, CSRF checks, refresh-token rotation, and migration idempotency covered by tests.
3. Prefer Java 8 language/API features even when building with a newer local toolchain.
4. If a future PostgreSQL variant is needed, add a profile-specific mapper/migration implementation behind the repository boundary; do not alter SQLite defaults.

## Verification

Run `bun run verify` with `JAVA8_HOME` set, then `bun run test:e2e`. Inspect `git diff --check` and confirm no Hono backend, Drizzle, SSG, SSR, or Docker dependency was introduced.
