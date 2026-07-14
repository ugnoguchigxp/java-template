import { defineConfig } from "vitest/config";

export default defineConfig({
	test: {
		include: [
			"web/**/*.test.ts",
			"shared/**/*.test.ts",
			"scripts/**/*.test.ts",
		],
		coverage: {
			provider: "v8",
			reporter: ["text", "html"],
			include: [
				"web/src/routes/route-access.ts",
				"web/src/showcase-table-search.ts",
				"shared/**/*.ts",
			],
			exclude: ["**/*.test.ts", "**/*.spec.ts"],
			thresholds: {
				lines: 80,
				functions: 80,
				branches: 80,
				statements: 80,
			},
		},
	},
});
