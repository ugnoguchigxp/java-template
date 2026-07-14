import { loadDotEnv, java8Env, run } from "./env";
await loadDotEnv();
let args = process.argv.slice(2);
const env = java8Env();
if (args.includes("--password-stdin")) {
	const password = (await new Response(Bun.stdin.stream()).text()).replace(
		/\r?\n$/,
		"",
	);
	env.ADMIN_PASSWORD = password;
	args = args.filter((arg) => arg !== "--password-stdin");
}
const quoteGradleArg = (value: string) =>
	`"${value.replace(/\\/g, "\\\\").replace(/"/g, '\\"')}"`;
const gradleArgs = args
	.map((arg) => (arg.startsWith("--") ? arg : quoteGradleArg(arg)))
	.join(" ");
process.exit(
	await run(
		["./gradlew", "--no-daemon", "authCreateAdmin", `--args=${gradleArgs}`],
		env,
	),
);
