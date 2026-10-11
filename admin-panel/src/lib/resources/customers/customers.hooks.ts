"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { BusinessScope } from "@/lib/auth/scope";
import { queryKeys } from "@/lib/query/queryKeys";
import type { DataSource, Id } from "@/lib/api/types";
import {
	createCustomer,
	getCustomer,
	listCustomers,
	removeCustomer,
	updateCustomer,
	type CustomerPageQuery,
} from "./customers.api";
import type { CustomerInput } from "./customers.types";

/** Live: customers are a real, business-scoped resource (DB-03), listed across salons (BE-15). */
export const source: DataSource = "live";

/**
 * One page of customers in the caller's scope: their own salon's, or for a platform caller every
 * salon's unless the query names one. An unlinked ADMIN or EMPLOYEE has no salon to read.
 */
export function useCustomerPage(scope: BusinessScope, query: Omit<CustomerPageQuery, "businessId"> & { businessId?: Id | null }) {
	const enabled = scope.kind !== "unresolved";
	const pageQuery: CustomerPageQuery = {
		...query,
		businessId: scope.kind === "business" ? scope.businessId : (query.businessId ?? null),
	};
	const result = useQuery({
		queryKey: queryKeys.customers.page(pageQuery),
		queryFn: ({ signal }) => listCustomers(pageQuery, { signal }),
		placeholderData: keepPreviousData,
		enabled,
	});
	return { ...result, isPending: enabled && result.isPending, source };
}

export function useCustomer(customerId: Id | null) {
	const result = useQuery({
		queryKey: queryKeys.customers.detail(customerId ?? -1),
		queryFn: ({ signal }) => getCustomer(customerId as Id, { signal }),
		enabled: customerId != null,
	});
	return { ...result, data: result.data ?? null, isPending: customerId != null && result.isPending, source };
}

function useCustomerMutation<TVars, TResult>(mutationFn: (vars: TVars) => Promise<TResult>) {
	const qc = useQueryClient();
	return useMutation({
		mutationFn,
		onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.customers.all }),
	});
}

export function useCreateCustomer() {
	return useCustomerMutation(({ businessId, input }: { businessId: Id; input: CustomerInput }) =>
		createCustomer(businessId, input),
	);
}

export function useUpdateCustomer() {
	return useCustomerMutation(
		({ businessId, id, input }: { businessId: Id; id: Id; input: CustomerInput }) =>
			updateCustomer(businessId, id, input),
	);
}

export function useDeleteCustomer() {
	return useCustomerMutation(({ businessId, id }: { businessId: Id; id: Id }) =>
		removeCustomer(businessId, id),
	);
}
