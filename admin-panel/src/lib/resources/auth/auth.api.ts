import { apiClient } from "@/lib/api/client";
import { endpoints } from "@/lib/api/endpoints";
import { isApiError } from "@/lib/api/errors";
import type { AuthUser } from "./auth.types";
import { toAuthUser, type AuthUserDto } from "./auth.mappers";

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
