import { loadDotEnv, java8Env } from "./env";

await loadDotEnv();
const env = java8Env();
const backend = Bun.spawn(["./gradlew", "--no-daemon", "bootRun"], {
	stdout: "inherit",
	stderr: "inherit",
	env,
});
const frontend = Bun.spawn(["bunx", "--bun", "vite"], {
	stdout: "inherit",
	stderr: "inherit",
	env: process.env,
});

let stopping = false;
const stop = async (signal: "SIGINT" | "SIGTERM") => {
	if (stopping) return;
	stopping = true;
	backend.kill(signal);
	frontend.kill(signal);
	await Promise.all([backend.exited, frontend.exited]);
};

process.on("SIGINT", () => void stop("SIGINT"));
process.on("SIGTERM", () => void stop("SIGTERM"));

const result = await Promise.race([
	backend.exited.then((code) => ({ code })),
	frontend.exited.then((code) => ({ code })),
]);
if (!stopping) await stop("SIGTERM");
process.exit(result.code === 0 || result.code === null ? 0 : result.code);
