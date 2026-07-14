import {
	useMutation,
	useQuery,
	useQueryClient,
	type UseMutationOptions,
} from "@tanstack/react-query";
import type {
	AuthResponse,
	AuthSessionUser,
	LoginInput,
	LogoutResponse,
} from "@shared/schemas/auth.schema";
import type { ProtectedProfileResponse } from "@shared/schemas/protected.schema";

export type AuthUser = AuthSessionUser;
export type LoginParams = LoginInput & { redirectTo?: string };
export type LoginResponse = AuthResponse;

export const UNAUTHORIZED_EVENT_NAME = "java25-sqlite-template:unauthorized";
export const authMeQueryKey = ["auth", "me"] as const;
export const protectedProfileQueryKey = ["protected", "profile"] as const;

type LoginMutationOptions = Omit<
	UseMutationOptions<LoginResponse, Error, LoginParams>,
	"mutationFn"
>;
type LogoutMutationOptions = Omit<
	UseMutationOptions<void, Error, void>,
	"mutationFn"
>;

let lastUnauthorizedEventAt = 0;

const notifyUnauthorized = () => {
	if (typeof window === "undefined") return;
	const now = Date.now();
	if (now - lastUnauthorizedEventAt < 500) return;
	lastUnauthorizedEventAt = now;
	window.dispatchEvent(new Event(UNAUTHORIZED_EVENT_NAME));
};

const requestPath = (input: RequestInfo | URL): string => {
	const url = input instanceof Request ? input.url : input.toString();
	const base =
		typeof window === "undefined" ? "http://localhost" : window.location.origin;
	return new URL(url, base).pathname;
};

const isAuthPath = (path: string) => path.startsWith("/api/auth/");
const shouldRefresh = (path: string) => !isAuthPath(path);

const readCookie = (name: string): string | null => {
	if (typeof document === "undefined") return null;
	const prefix = `${name}=`;
	const value = document.cookie
		.split("; ")
		.find((item) => item.startsWith(prefix));
	return value ? decodeURIComponent(value.slice(prefix.length)) : null;
};

let csrfPromise: Promise<string> | null = null;
const ensureCsrf = async (): Promise<string> => {
	const existing = readCookie("XSRF-TOKEN");
	if (existing) return existing;
	if (!csrfPromise) {
		csrfPromise = fetch("/api/csrf", { credentials: "include" })
			.then(async (response) => {
				if (!response.ok)
					throw new Error("Failed to initialize CSRF protection.");
				const body = (await response.json()) as { token?: string };
				if (!body.token) throw new Error("CSRF token was not returned.");
				return body.token;
			})
			.finally(() => {
				csrfPromise = null;
			});
	}
	return csrfPromise;
};

const parseErrorMessage = async (response: Response): Promise<string> => {
	let message = `Request failed: ${response.status}`;
	try {
		const data = (await response.json()) as { message?: string };
		if (data.message) message = data.message;
	} catch {
		// Keep the status-derived message for non-JSON responses.
	}
	return message;
};

const request = async (
	input: RequestInfo | URL,
	init: RequestInit = {},
): Promise<Response> => {
	const path = requestPath(input);
	const headers = new Headers(init.headers);
	headers.set("Accept", "application/json");
	if (init.body && !headers.has("Content-Type"))
		headers.set("Content-Type", "application/json");
	const method = (init.method ?? "GET").toUpperCase();
	if (!["GET", "HEAD", "OPTIONS", "TRACE"].includes(method)) {
		headers.set("X-XSRF-TOKEN", await ensureCsrf());
	}
	const execute = () =>
		fetch(input, { ...init, headers, credentials: "include" });
	let response = await execute();
	if (response.status === 401 && shouldRefresh(path)) {
		const refresh = await request("/api/auth/refresh", { method: "POST" });
		if (refresh.ok) response = await execute();
	}
	if (response.status === 401 && shouldRefresh(path)) notifyUnauthorized();
	return response;
};

const parseJson = async <T>(response: Response): Promise<T> => {
	if (!response.ok) throw new Error(await parseErrorMessage(response));
	return (await response.json()) as T;
};

export async function login(params: LoginParams): Promise<LoginResponse> {
	return parseJson<LoginResponse>(
		await request("/api/auth/login", {
			method: "POST",
			body: JSON.stringify({ email: params.email, password: params.password }),
		}),
	);
}

export async function logout(): Promise<void> {
	await parseJson<LogoutResponse>(
		await request("/api/auth/logout", { method: "POST" }),
	);
}

export async function fetchMe(): Promise<AuthUser | null> {
	const response = await request("/api/auth/me");
	if (response.status === 401) return null;
	return (await parseJson<AuthResponse>(response)).user;
}

export type ProtectedProfile = ProtectedProfileResponse["profile"];

export async function fetchProtectedProfile(): Promise<ProtectedProfile> {
	return (
		await parseJson<ProtectedProfileResponse>(
			await request("/api/protected/profile"),
		)
	).profile;
}

export function useCurrentUserQuery(enabled = true) {
	return useQuery<AuthUser | null, Error>({
		queryKey: authMeQueryKey,
		queryFn: fetchMe,
		enabled,
	});
}

export function useProtectedProfileQuery(enabled = true) {
	return useQuery<ProtectedProfile, Error>({
		queryKey: protectedProfileQueryKey,
		queryFn: fetchProtectedProfile,
		enabled,
	});
}

export function useLoginMutation(options?: LoginMutationOptions) {
	const queryClient = useQueryClient();
	return useMutation<LoginResponse, Error, LoginParams>({
		mutationFn: login,
		...options,
		onSuccess: async (response, variables, onMutateResult, context) => {
			queryClient.setQueryData(authMeQueryKey, response.user);
			await options?.onSuccess?.(response, variables, onMutateResult, context);
		},
	});
}

export function useLogoutMutation(options?: LogoutMutationOptions) {
	const queryClient = useQueryClient();
	return useMutation<void, Error, void>({
		mutationFn: logout,
		...options,
		onSettled: async (data, error, variables, onMutateResult, context) => {
			queryClient.setQueryData(authMeQueryKey, null);
			await options?.onSettled?.(
				data,
				error,
				variables,
				onMutateResult,
				context,
			);
		},
	});
}
