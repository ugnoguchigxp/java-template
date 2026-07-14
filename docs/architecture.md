# Architecture

The application is intentionally split into three small boundaries:

1. `web/` is the copied React/Vite frontend. `web/src/api.ts` is the only transport adapter and uses same-origin `fetch` against `/api`.
2. `src/main/java` exposes Spring MVC controllers and keeps authentication, cookies, CSRF, and token rotation in service/filter classes.
3. `UserRepository` and `RefreshTokenRepository` hide MyBatis mapper details. The active mapper XML targets SQLite and the migration runner reads `db/migration`.

In development Vite proxies `/api` to Spring Boot. In a packaged JAR, `processResources` copies `dist-web` to `static`, and `SpaController` provides the SPA fallback for client routes. There is no SSR/SSG path.
