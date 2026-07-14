# Security boundary

- Access and refresh tokens are HttpOnly, `SameSite=Lax` cookies.
- State-changing `/api/**` requests (POST/PUT/PATCH/DELETE) require the `X-XSRF-TOKEN` header to match the `XSRF-TOKEN` cookie.
- Refresh tokens are stored hashed in SQLite and are deleted before a replacement token is issued (rotation/replay prevention).
- Passwords are encoded with Spring Security's BCrypt encoder.
- Production startup requires an explicit `JWT_SECRET` and rejects wildcard CORS origins.
- Production startup also requires `AUTH_COOKIE_SECURE=true`.
- The default development seed is only for local SQLite use and never runs automatically in production.
- Protected profile requests re-check that the user still exists and is active.
