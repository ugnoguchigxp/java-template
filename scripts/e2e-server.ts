import { mkdir, rm } from "node:fs/promises";
import { loadDotEnv, java8Env, run } from "./env";

await loadDotEnv();
await mkdir("tmp", { recursive: true });
await rm("tmp/e2e.sqlite", { force: true });
await rm("tmp/e2e.sqlite-shm", { force: true });
await rm("tmp/e2e.sqlite-wal", { force: true });

const env: Record<string, string> = {
	...java8Env(),
	SPRING_PROFILES_ACTIVE: "development",
	HOST: "127.0.0.1",
	DATABASE_URL: "tmp/e2e.sqlite",
	JWT_SECRET: "e2e-secret-that-is-at-least-32-characters",
	PORT: "5174",
	APP_URL: "http://127.0.0.1:5174",
	CORS_ORIGINS: "http://127.0.0.1:5174",
	AUTH_COOKIE_SECURE: "false",
	AUTH_COOKIE_SAME_SITE: "Lax",
	ADMIN_PASSWORD: "password123456",
};
const migrationExit = await run(["./gradlew", "--no-daemon", "dbMigrate"], env);
if (migrationExit !== 0) process.exit(migrationExit);
const seedExit = await run(
	[
		"./gradlew",
		"--no-daemon",
		"authCreateAdmin",
		"--args=--email admin@example.com --name Admin",
	],
	env,
);
if (seedExit !== 0) process.exit(seedExit);

const jarExit = await run(["./gradlew", "--no-daemon", "bootJar"], env);
if (jarExit !== 0) process.exit(jarExit);

const child = Bun.spawn(
	[
		`${env.JAVA_HOME}/bin/java`,
		"-jar",
		"build/libs/java8-sqlite-template-0.1.0.jar",
		"--server.port=5174",
	],
	{
		stdout: "inherit",
		stderr: "inherit",
		env,
	},
);
process.on("SIGINT", () => child.kill("SIGINT"));
process.on("SIGTERM", () => child.kill("SIGTERM"));
process.exit(await child.exited);
