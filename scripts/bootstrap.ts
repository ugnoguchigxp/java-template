import { loadDotEnv, java25Env, run } from "./env";

const envFile = Bun.file(".env");
if (!(await envFile.exists())) {
	await Bun.write(".env", await Bun.file(".env.example").text());
}
await loadDotEnv();
if (!process.env.DATABASE_URL?.startsWith("jdbc:postgresql://")) {
	throw new Error(
		"DATABASE_URL must be a PostgreSQL JDBC URL for this variant.",
	);
}
const installExit = await run(["bun", "install", "--frozen-lockfile"]);
if (installExit !== 0) process.exit(installExit);
const migrateExit = await run(
	["./gradlew", "--no-daemon", "dbMigrate"],
	java25Env(),
);
process.exit(migrateExit);
