export async function loadDotEnv(path = ".env"): Promise<void> {
	const file = Bun.file(path);
	if (!(await file.exists())) return;
	const text = await file.text();
	for (const line of text.split(/\r?\n/)) {
		const trimmed = line.trim();
		if (!trimmed || trimmed.startsWith("#")) continue;
		const separator = trimmed.indexOf("=");
		if (separator <= 0) continue;
		const key = trimmed.slice(0, separator).trim();
		const value = trimmed
			.slice(separator + 1)
			.trim()
			.replace(/^(['"])(.*)\1$/, "$2");
		if (!process.env[key]) process.env[key] = value;
	}
}

export function java8Env(): Record<string, string> {
	const java8Home = process.env.JAVA8_HOME;
	if (!java8Home) throw new Error("JAVA8_HOME must point to a Java 8 JDK.");
	return {
		...(process.env as Record<string, string>),
		JAVA_HOME: java8Home,
		PATH: `${java8Home}/bin:${process.env.PATH ?? ""}`,
	};
}

export async function run(
	command: string[],
	env = process.env,
): Promise<number> {
	const processHandle = Bun.spawn(command, {
		stdout: "inherit",
		stderr: "inherit",
		stdin: "inherit",
		env,
	});
	return processHandle.exited;
}
