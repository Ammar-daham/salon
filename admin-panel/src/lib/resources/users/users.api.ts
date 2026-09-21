import { apiClient } from "@/lib/api/client";
import { endpoints } from "@/lib/api/endpoints";
import type { Id, ListParams, Repository } from "@/lib/api/types";
import type { CreateUserInput, UpdateUserInput, User } from "./users.types";

/**
 * Users are the one resource with no field-name asymmetry: the backend's User
 * has a no-arg constructor and public fields, so Jackson binds it field-based
 * and it is camelCase in both directions. Don't "fix" it to match the others.
 *
 * Known limits this repository cannot paper over:
 *  - the list is platform-wide with no scoping or filtering
 *  - PUT only writes first_name, last_name and role; email and password can
 *    never be edited, and there is no password-reset endpoint
 */
export const usersRepository: Repository<User, CreateUserInput, UpdateUserInput> = {
	source: "live",

	async list(params?: ListParams) {
		const { data } = await apiClient.get<User[]>(endpoints.users.root, {
			signal: params?.signal,
		});
		return data ?? [];
	},

	async get(id: Id, params?: ListParams) {
		const { data } = await apiClient.get<User>(endpoints.users.byId(id), {
			signal: params?.signal,
		});
		return data;
	},

	async create(input: CreateUserInput) {
		const { data } = await apiClient.post<User>(endpoints.users.root, input);
		return data;
	},

	async update(id: Id, input: UpdateUserInput) {
		await apiClient.put(endpoints.users.byId(id), input);
	},

	/**
	 * DELETE requires a body here too, and its shape matters: deleteUserById
	 * runs the DELETE first, then branches on `contacts` / `addresses` to decide
	 * what to return. Echo the real record so child rows are handled, and never
	 * trust the response — deleting a nonexistent user still returns 200.
	 */
	async remove(id: Id) {
		const { data } = await apiClient.get<User>(endpoints.users.byId(id));
		await apiClient.delete(endpoints.users.byId(id), {
			data: { ...data, contacts: [], addresses: [] },
		});
	},
};

/** Convenience wrapper kept for the staff-creation form. */
export async function createUser(input: CreateUserInput): Promise<User> {
	return usersRepository.create(input);
}
