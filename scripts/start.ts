import { loadDotEnv, java25Env } from "./env";
await loadDotEnv();
const child = Bun.spawn(["./gradlew", "--no-daemon", "bootRun"], {
	stdout: "inherit",
	stderr: "inherit",
	stdin: "inherit",
	env: java25Env(),
});
process.on("SIGINT", () => child.kill("SIGINT"));
process.on("SIGTERM", () => child.kill("SIGTERM"));
process.exit(await child.exited);
