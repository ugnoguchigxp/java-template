import { mkdir } from "node:fs/promises";
import { loadDotEnv, java25Env, run } from "./env";

const envFile = Bun.file(".env");
if (!(await envFile.exists())) {
	await Bun.write(".env", await Bun.file(".env.example").text());
}
await loadDotEnv();
await mkdir("data", { recursive: true });
const installExit = await run(["bun", "install", "--frozen-lockfile"]);
if (installExit !== 0) process.exit(installExit);
const migrateExit = await run(
	["./gradlew", "--no-daemon", "dbMigrate"],
	java25Env(),
);
process.exit(migrateExit);
