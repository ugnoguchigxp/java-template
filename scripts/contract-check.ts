import {
	authResponseSchema,
	logoutResponseSchema,
} from "../shared/schemas/auth.schema";
import { protectedProfileResponseSchema } from "../shared/schemas/protected.schema";

const openapi = await Bun.file("api-contract/openapi.yaml").text();
for (const path of [
	"/api/health",
	"/api/csrf",
	"/api/auth/login",
	"/api/auth/refresh",
	"/api/auth/logout",
	"/api/auth/me",
	"/api/protected/profile",
]) {
	if (!openapi.includes(`  ${path}:`)) {
		throw new Error(`OpenAPI path is missing: ${path}`);
	}
}

authResponseSchema.parse(
	JSON.parse(await Bun.file("api-contract/fixtures/auth-response.json").text()),
);
logoutResponseSchema.parse(
	JSON.parse(
		await Bun.file("api-contract/fixtures/logout-response.json").text(),
	),
);
protectedProfileResponseSchema.parse(
	JSON.parse(
		await Bun.file(
			"api-contract/fixtures/protected-profile-response.json",
		).text(),
	),
);
console.log("API contract paths and fixtures are valid.");
