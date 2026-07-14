import { loadDotEnv, java8Env, run } from "./env";
await loadDotEnv();
const env = java8Env();
const jarExit = await run(["./gradlew", "--no-daemon", "bootJar"], env);
if (jarExit !== 0) process.exit(jarExit);
const child = Bun.spawn(
	[
		`${env.JAVA_HOME}/bin/java`,
		"-jar",
		"build/libs/java8-postgresql-template-0.1.0.jar",
	],
	{
		stdout: "inherit",
		stderr: "inherit",
		env,
	},
);
let healthy = false;
for (let attempt = 0; attempt < 30; attempt += 1) {
	await Bun.sleep(1000);
	try {
		const response = await fetch("http://127.0.0.1:8080/api/health");
		if (response.ok) {
			healthy = true;
			break;
		}
	} catch {
		// Keep polling until the timeout.
	}
}
child.kill("SIGTERM");
await child.exited;
if (!healthy) {
	console.error("Application did not become healthy.");
	process.exit(1);
}
process.exit(0);
