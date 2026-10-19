import { apiClient } from "@/lib/api/client";
import { endpoints } from "@/lib/api/endpoints";
import { pageParams, toPage, type PageDto } from "@/lib/api/paging";
import type { Id, ListParams, PageQuery, Repository } from "@/lib/api/types";
import type { Role } from "@/lib/resources/auth/auth.types";
import type { CreateUserInput, UpdateUserInput, User } from "./users.types";
import { toCreateUserRequest, toUpdateUserRequest, toUser, type UserDto } from "./users.mappers";

/** sort is name, email, role or created_at; q matches the name or email. */
export interface UserPageQuery extends PageQuery {
	role?: Role;
}

/**
 * The list is every user for a SUPER_ADMIN and the caller's own salon's for an ADMIN, decided by
 * the server. update writes only first_name and last_name, and changeUserRole the role; email can
 * never be edited, and a password only by its owner, through auth.api.
 */
export const usersRepository: Repository<User, CreateUserInput, UpdateUserInput, UserPageQuery> = {
	source: "live",

	async list(query: UserPageQuery, params?: ListParams) {
		const { data } = await apiClient.get<PageDto<UserDto>>(endpoints.users.root, {
			params: pageParams(query, { role: query.role }),
			signal: params?.signal,
		});
		return toPage(data, toUser);
	},

	async get(id: Id, params?: ListParams) {
		const { data } = await apiClient.get<UserDto>(endpoints.users.byId(id), {
			signal: params?.signal,
		});
		return toUser(data);
	},

	async create(input: CreateUserInput) {
		const { data } = await apiClient.post<UserDto>(endpoints.users.root, toCreateUserRequest(input));
		return toUser(data);
	},

	async update(id: Id, input: UpdateUserInput) {
		const { data } = await apiClient.put<UserDto>(endpoints.users.byId(id), toUpdateUserRequest(input));
		return toUser(data);
	},

	async remove(id: Id) {
		await apiClient.delete(endpoints.users.byId(id));
	},
};

/** ADMIN and SUPER_ADMIN only. Answers with the user as stored. */
export async function changeUserRole(id: Id, role: Role): Promise<User> {
	const { data } = await apiClient.put<UserDto>(endpoints.users.role(id), { role });
	return toUser(data);
}

/** Convenience wrapper kept for the staff-creation form. */
export async function createUser(input: CreateUserInput): Promise<User> {
	return usersRepository.create(input);
}
