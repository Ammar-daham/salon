import { Role } from "./auth";
import { api } from "./axios";

export interface CreateUserInput {
	firstName: string;
	lastName: string;
	email: string;
	password: string;
	role: Role;
	// Required when the caller is a SUPER_ADMIN (they aren't tied to a single business).
	// Ignored by the backend for any other caller, which is always scoped to their own business.
	businessId?: number;
}

export interface CreatedUser {
	id: number;
	firstName: string;
	lastName: string;
	email: string;
	role: Role;
}

export async function createUser(input: CreateUserInput): Promise<CreatedUser> {
	const { data } = await api.post<CreatedUser>("/api/v1/users", input);
	return data;
}
