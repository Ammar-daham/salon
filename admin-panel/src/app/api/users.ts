import { Role } from "./auth";
import { api } from "./axios";

export interface CreateUserInput {
	firstName: string;
	lastName: string;
	email: string;
	password: string;
	role: Role;
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
