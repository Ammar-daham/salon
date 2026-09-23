import axios from "axios";
import { normalizeError } from "./errors";

/**
 * Auth is a stateful Spring session cookie (JSESSIONID), not a JWT — so every
 * request must send credentials, and the browser handles the token for us.
 * The server's CORS config allows exactly one origin (http://localhost:3000 by
 * default) with allowCredentials, so this base URL has to match it.
 */
export const apiClient = axios.create({
	baseURL: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080",
	withCredentials: true,
});

type UnauthorizedHandler = () => void;

let onUnauthorized: UnauthorizedHandler | null = null;

/**
 * Registered once by AuthProvider. The 401 path has to clear session identity
 * synchronously, which is why auth stays a context rather than a query cache
 * entry.
 */
export function setUnauthorizedHandler(handler: UnauthorizedHandler | null) {
	onUnauthorized = handler;
}

/** Requests that are *expected* to 401 when signed out, and so must not trigger
 *  the global sign-out path. `/auth/me` is the boot probe; `/auth/login` 401s on
 *  bad credentials and the form shows that inline. */
const SILENT_401 = [
	"/api/v1/auth/me",
	"/api/v1/auth/login",
];

apiClient.interceptors.response.use(
	(response) => response,
	(error) => {
		const normalized = normalizeError(error);
		const url = error?.config?.url ?? "";

		if (
			normalized.kind === "unauthorized" &&
			!SILENT_401.some((path) => url.includes(path))
		) {
			onUnauthorized?.();
		}

		return Promise.reject(normalized);
	},
);
