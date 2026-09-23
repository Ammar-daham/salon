import { AxiosError } from "axios";

/**
 * The backend returns a uniform envelope from both MVC handlers and the security
 * filters (SecurityResponseWriter hand-builds the same JSON):
 *
 *   { errorCode, message, status, timestamp, path }
 *
 * Two traps, both load-bearing:
 *  - `status` is a Spring HttpStatus *enum*, so Jackson serialises it as the NAME
 *    ("NOT_FOUND"), not a number. Never read a numeric status from the body.
 *  - `errorCode` is an ad-hoc vocabulary and partly numeric ("400"), because
 *    ErrorCode's constructor discards its message/code arguments.
 *
 * So: key off the HTTP status of the response, and treat the body as message-only.
 */
export interface ApiErrorBody {
	errorCode?: string;
	message?: string;
	status?: string;
	timestamp?: string;
	path?: string;
}

export type ApiErrorKind =
	| "unauthorized"
	| "forbidden"
	| "not-found"
	| "conflict"
	| "validation"
	| "server"
	| "network"
	| "unknown";

export class ApiError extends Error {
	readonly kind: ApiErrorKind;
	readonly status: number | null;
	readonly errorCode?: string;
	readonly path?: string;

	constructor(
		message: string,
		kind: ApiErrorKind,
		status: number | null,
		body?: ApiErrorBody,
	) {
		super(message);
		this.name = "ApiError";
		this.kind = kind;
		this.status = status;
		this.errorCode = body?.errorCode;
		this.path = body?.path;
	}
}

export function isApiError(error: unknown): error is ApiError {
	return error instanceof ApiError;
}

function kindFromStatus(status: number): ApiErrorKind {
	if (status === 401) return "unauthorized";
	if (status === 403) return "forbidden";
	if (status === 404) return "not-found";
	if (status === 409) return "conflict";
	if (status === 400 || status === 422) return "validation";
	if (status >= 500) return "server";
	return "unknown";
}

const FALLBACK: Record<ApiErrorKind, string> = {
	unauthorized: "Your session has expired. Please sign in again.",
	forbidden: "You don't have permission to do that.",
	"not-found": "We couldn't find what you were looking for.",
	conflict: "That conflicts with something that already exists.",
	validation: "Some of the details weren't accepted. Please check and try again.",
	server: "The server ran into a problem. Please try again.",
	network: "Couldn't reach the server. Check your connection and try again.",
	unknown: "Something went wrong. Please try again.",
};

/** Turn anything thrown by axios (or by us) into a typed ApiError. */
export function normalizeError(error: unknown): ApiError {
	if (isApiError(error)) return error;

	if (error instanceof AxiosError) {
		if (!error.response) {
			return new ApiError(FALLBACK.network, "network", null);
		}
		const status = error.response.status;
		const body = error.response.data as ApiErrorBody | undefined;
		const kind = kindFromStatus(status);
		return new ApiError(body?.message?.trim() || FALLBACK[kind], kind, status, body);
	}

	if (error instanceof Error) {
		return new ApiError(error.message || FALLBACK.unknown, "unknown", null);
	}

	return new ApiError(FALLBACK.unknown, "unknown", null);
}

/** Message suitable for showing directly in a toast or inline form error. */
export function getErrorMessage(error: unknown): string {
	return normalizeError(error).message;
}
