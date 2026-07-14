import { loadDotEnv, java25Env, run } from "./env";
await loadDotEnv();
process.exit(await run(["./gradlew", "--no-daemon", "dbMigrate"], java25Env()));
