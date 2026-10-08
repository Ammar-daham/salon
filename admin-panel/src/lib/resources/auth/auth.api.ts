import { apiClient } from "@/lib/api/client";
import { endpoints } from "@/lib/api/endpoints";
import { isApiError } from "@/lib/api/errors";
import type { AuthUser, ChangePasswordInput } from "./auth.types";
import { toAuthUser, toChangePasswordRequest, type AuthUserDto } from "./auth.mappers";

export async function login(email: string, password: string): Promise<AuthUser> {
	const { data } = await apiClient.post<AuthUserDto>(endpoints.auth.login, {
		email,
		password,
	});
	return toAuthUser(data);
}

export async function logout(): Promise<void> {
	await apiClient.post(endpoints.auth.logout);
}

/**
 * The signed-in user's own password. A wrong current password is a 400, not a 401, so it
 * doesn't sign them out; it counts towards the sign-in lockout, which is a 429. Every other
 * session of theirs is signed out, and this one carries on under a new session id.
 */
export async function changePassword(input: ChangePasswordInput): Promise<void> {
	await apiClient.post(endpoints.auth.changePassword, toChangePasswordRequest(input));
}

/** Returns null when signed out rather than throwing — this is the boot probe
 *  and a 401 here is an ordinary answer, not an error. */
export async function getCurrentUser(): Promise<AuthUser | null> {
	try {
		const { data } = await apiClient.get<AuthUserDto>(endpoints.auth.me);
		return toAuthUser(data);
	} catch (error) {
		if (isApiError(error) && error.kind === "unauthorized") return null;
		throw error;
	}
}
