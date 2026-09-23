import type { CreateUserInput, UpdateUserInput, User } from "./users.types";

/**
 * Wire shape, verified against a running backend. Users are snake_case on the
 * wire in both directions, same as every other resource — `business_id` is
 * WRITE_ONLY, so it is accepted on create but never comes back in a response.
 *
 * Everything past this file speaks one camelCase domain model.
 */
export interface UserDto {
	id: number;
	first_name: string;
	last_name: string;
	email: string | null;
	role: User["role"];
	created_at: string;
	updated_at: string | null;
}

export function toUser(dto: UserDto): User {
	return {
		id: dto.id,
		firstName: dto.first_name,
		lastName: dto.last_name,
		email: dto.email,
		role: dto.role,
		createdAt: dto.created_at,
		updatedAt: dto.updated_at ?? null,
	};
}

export function toCreateUserRequest(input: CreateUserInput) {
	return {
		first_name: input.firstName,
		last_name: input.lastName,
		email: input.email,
		password: input.password,
		role: input.role,
		...(input.businessId != null ? { business_id: input.businessId } : {}),
	};
}

export function toUpdateUserRequest(input: UpdateUserInput) {
	return {
		first_name: input.firstName,
		last_name: input.lastName,
		role: input.role,
	};
}
