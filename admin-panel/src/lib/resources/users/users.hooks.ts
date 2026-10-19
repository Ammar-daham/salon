"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/query/queryKeys";
import type { Id } from "@/lib/api/types";
import { changeUserRole, usersRepository, type UserPageQuery } from "./users.api";
import type { CreateUserInput, UpdateUserInput } from "./users.types";

/** One page of users; the last page stays on screen while the next one loads. */
export function useUserPage(query: UserPageQuery) {
	return useQuery({
		queryKey: queryKeys.users.page(query),
		queryFn: ({ signal }) => usersRepository.list(query, { signal }),
		placeholderData: keepPreviousData,
	});
}

export function useUser(id: Id | null) {
	return useQuery({
		queryKey: queryKeys.users.detail(id ?? -1),
		queryFn: ({ signal }) => usersRepository.get(id as Id, { signal }),
		enabled: id != null,
	});
}

export function useCreateUser() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: (input: CreateUserInput) => usersRepository.create(input),
		onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.users.all }),
	});
}

export function useUpdateUser() {
	const qc = useQueryClient();
	return useMutation({
		// The name and the role have separate endpoints, and the role is only sent when it changes.
		mutationFn: async ({ id, input }: { id: Id; input: UpdateUserInput }) => {
			const renamed = await usersRepository.update(id, input);
			return renamed.role === input.role ? renamed : changeUserRole(id, input.role);
		},
		// Each answers with the user as stored, so their detail needs no refetch; the lists do.
		onSuccess: (user, { id }) => {
			qc.setQueryData(queryKeys.users.detail(id), user);
			qc.invalidateQueries({ queryKey: queryKeys.users.list() });
		},
		// The name may have been saved before the role was refused.
		onError: (_error, { id }) => {
			qc.invalidateQueries({ queryKey: queryKeys.users.detail(id) });
			qc.invalidateQueries({ queryKey: queryKeys.users.list() });
		},
	});
}

export function useDeleteUser() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: (id: Id) => usersRepository.remove(id),
		onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.users.all }),
	});
}
