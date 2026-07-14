import { loadDotEnv, java8Env, run } from "./env";
await loadDotEnv();
process.exit(await run(["./gradlew", "--no-daemon", "dbMigrate"], java8Env()));
