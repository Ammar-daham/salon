import { AxiosError } from "axios";
import { api } from "./axios";

export type Role = "ADMIN" | "SUPER_ADMIN" | "USER";

export interface AuthUser {
	id: number;
	firstName: string;
	lastName: string;
	email: string;
	role: Role;
}

interface ApiErrorBody {
	errorCode?: string;
	message?: string;
}

export function getAuthErrorMessage(error: unknown): string {
	if (error instanceof AxiosError) {
		const body = error.response?.data as ApiErrorBody | undefined;
		if (body?.message) return body.message;
	}
	return "Something went wrong. Please try again.";
}

export async function login(email: string, password: string): Promise<AuthUser> {
	const { data } = await api.post<AuthUser>("/api/v1/auth/login", { email, password });
	return data;
}

export async function logout(): Promise<void> {
	await api.post("/api/v1/auth/logout");
}

export async function getCurrentUser(): Promise<AuthUser | null> {
	try {
		const { data } = await api.get<AuthUser>("/api/v1/auth/me");
		return data;
	} catch (error) {
		if (error instanceof AxiosError && error.response?.status === 401) {
			return null;
		}
		throw error;
	}
}
