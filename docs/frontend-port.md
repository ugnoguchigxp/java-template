# Frontend port record

The frontend was copied from `../hono-standard` at source commit `83bf7d951bd8`. At copy time the source worktree had an unrelated `M LLM_CONTEXT.md` change; that file was not copied. The copied surface includes the React routes, styling, Design System, Showcase, shared schemas, and their tests.

The Hono server/API implementation, Drizzle database layer, `entry-server.tsx`, and SSG/SSR build scripts were intentionally excluded. The only integration change is `web/src/api.ts`, which replaces the Hono client with a same-origin fetch adapter for the Java API while preserving the existing React Query hooks and response shapes.
