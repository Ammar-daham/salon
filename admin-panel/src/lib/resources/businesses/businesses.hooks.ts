"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/query/queryKeys";
import type { Id } from "@/lib/api/types";
import { businessesRepository, listAllBusinesses, type BusinessPageQuery } from "./businesses.api";
import type { BusinessInput } from "./businesses.types";

/** Every salon the caller can see, by name, e.g. for a salon picker. */
export function useBusinesses(enabled = true) {
	return useQuery({
		queryKey: queryKeys.businesses.listAll(),
		queryFn: ({ signal }) => listAllBusinesses({ signal }),
		enabled,
	});
}

/** One page of salons; the last page stays on screen while the next one loads. */
export function useBusinessPage(query: BusinessPageQuery) {
	return useQuery({
		queryKey: queryKeys.businesses.page(query),
		queryFn: ({ signal }) => businessesRepository.list(query, { signal }),
		placeholderData: keepPreviousData,
	});
}

export function useBusiness(id: Id | null) {
	return useQuery({
		queryKey: queryKeys.businesses.detail(id ?? -1),
		queryFn: ({ signal }) => businessesRepository.get(id as Id, { signal }),
		enabled: id != null,
	});
}

export function useCreateBusiness() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: (input: BusinessInput) => businessesRepository.create(input),
		onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.businesses.all }),
	});
}

export function useUpdateBusiness() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: ({ id, input }: { id: Id; input: BusinessInput }) =>
			businessesRepository.update(id, input),
		// The PUT returns no body, so refetch rather than trusting a local merge.
		onSuccess: (_result, { id }) => {
			qc.invalidateQueries({ queryKey: queryKeys.businesses.detail(id) });
			qc.invalidateQueries({ queryKey: queryKeys.businesses.list() });
		},
	});
}

export function useDeleteBusiness() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: (id: Id) => businessesRepository.remove(id),
		onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.businesses.all }),
	});
}
