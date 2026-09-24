import { apiClient } from "@/lib/api/client";
import { endpoints } from "@/lib/api/endpoints";
import type { Id, ListParams, Repository } from "@/lib/api/types";
import type { CreateUserInput, UpdateUserInput, User } from "./users.types";
import { toCreateUserRequest, toUpdateUserRequest, toUser, type UserDto } from "./users.mappers";

/**
 * Known limits this repository cannot paper over:
 *  - the list is platform-wide with no scoping or filtering
 *  - PUT only writes first_name, last_name and role; email and password can
 *    never be edited, and there is no password-reset endpoint
 */
export const usersRepository: Repository<User, CreateUserInput, UpdateUserInput> = {
	source: "live",

	async list(params?: ListParams) {
		const { data } = await apiClient.get<UserDto[]>(endpoints.users.root, {
			signal: params?.signal,
		});
		return (data ?? []).map(toUser);
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
		await apiClient.put(endpoints.users.byId(id), toUpdateUserRequest(input));
	},

	/**
	 * FE-09: the backend now deletes the user's own children from the canonical
	 * record (BE-10), so a plain body-less DELETE is correct. This used to re-read
	 * the record and echo it back to drive the server's child cleanup.
	 */
	async remove(id: Id) {
		await apiClient.delete(endpoints.users.byId(id));
	},
};

/** Convenience wrapper kept for the staff-creation form. */
export async function createUser(input: CreateUserInput): Promise<User> {
	return usersRepository.create(input);
}
