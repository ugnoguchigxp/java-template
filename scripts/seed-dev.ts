import { loadDotEnv, java8Env, run } from "./env";
await loadDotEnv();
const profiles = (process.env.SPRING_PROFILES_ACTIVE ?? "")
	.split(",")
	.map((profile) => profile.trim().toLowerCase());
if (profiles.includes("production")) {
	throw new Error("seed:dev is not allowed in production.");
}
process.exit(await run(["./gradlew", "--no-daemon", "seedDev"], java8Env()));
