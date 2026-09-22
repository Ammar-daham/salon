"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/query/queryKeys";
import type { Id } from "@/lib/api/types";
import { usersRepository } from "./users.api";
import type { CreateUserInput, UpdateUserInput } from "./users.types";

export function useUsers() {
	return useQuery({
		queryKey: queryKeys.users.list(),
		queryFn: ({ signal }) => usersRepository.list({ signal }),
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
		mutationFn: ({ id, input }: { id: Id; input: UpdateUserInput }) =>
			usersRepository.update(id, input),
		// The PUT returns no body, so refetch rather than merging locally.
		onSuccess: (_r, { id }) => {
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
