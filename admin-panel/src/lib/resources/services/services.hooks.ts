"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/query/queryKeys";
import type { Id } from "@/lib/api/types";
import type { SalonServiceInput } from "@/lib/resources/businesses/businesses.types";
import { createService, listServices, removeService, updateService } from "./services.api";

export function useServices(businessId: Id | null) {
	return useQuery({
		// Derived from the business detail key: a service mutation invalidates the
		// business, and this list follows automatically.
		queryKey: queryKeys.businesses.services(businessId ?? -1),
		queryFn: ({ signal }) => listServices(businessId as Id, { signal }),
		enabled: businessId != null,
	});
}

/**
 * businessId travels in the mutation variables rather than being bound at hook
 * time, because a platform admin's services table spans salons — each row
 * belongs to a different one.
 *
 * Every mutation invalidates the owning *business*: the services list is a
 * projection of that response, not a resource with a list endpoint of its own.
 */
function useServiceMutation<TVars extends { businessId: Id }, TResult>(
	mutationFn: (vars: TVars) => Promise<TResult>,
) {
	const qc = useQueryClient();
	return useMutation({
		mutationFn,
		onSuccess: (_result, vars) => {
			qc.invalidateQueries({ queryKey: queryKeys.businesses.detail(vars.businessId) });
			qc.invalidateQueries({ queryKey: queryKeys.businesses.list() });
		},
	});
}

export function useCreateService() {
	return useServiceMutation(({ businessId, input }: { businessId: Id; input: SalonServiceInput }) =>
		createService(businessId, input),
	);
}

export function useUpdateService() {
	return useServiceMutation(
		({ businessId, id, input }: { businessId: Id; id: Id; input: SalonServiceInput }) =>
			updateService(businessId, id, input),
	);
}

export function useDeleteService() {
	return useServiceMutation(({ businessId, id }: { businessId: Id; id: Id }) =>
		removeService(businessId, id),
	);
}
