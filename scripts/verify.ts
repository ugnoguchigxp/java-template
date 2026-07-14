import { loadDotEnv, java25Env, run } from "./env";
await loadDotEnv();
if (!process.env.POSTGRES_TEST_URL?.startsWith("jdbc:postgresql://")) {
	throw new Error(
		"POSTGRES_TEST_URL must point to a disposable PostgreSQL test database.",
	);
}
const commands: string[][] = [
	["bun", "run", "typecheck"],
	["bun", "run", "lint"],
	["bun", "run", "format:check"],
	["bun", "run", "contract:check"],
	["bun", "run", "test:coverage"],
	["bun", "run", "build:web"],
	[
		"./gradlew",
		"--no-daemon",
		"check",
		"verifyJava25Runtime",
		"verifyJava25Bytecode",
		"bootJar",
	],
];
for (const command of commands) {
	const code = await run(
		command,
		command[0] === "./gradlew" ? java25Env() : process.env,
	);
	if (code !== 0) process.exit(code);
}
