"use client";

import { useMemo } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/context/AuthContext";
import { resolveBusinessScope, type BusinessScope } from "@/lib/auth/scope";
import { queryKeys } from "@/lib/query/queryKeys";
import type { DataSource, Id } from "@/lib/api/types";
import { createCustomer, listCustomers, removeCustomer, updateCustomer } from "./customers.api";
import type { CustomerInput } from "./customers.types";

/**
 * Live: customers are a real, business-scoped resource now (DB-03).
 *
 * The /customers/:id route carries no businessId, so useCustomer composes on top of
 * the caller's own list query, like useEmployee does. Unlike staff, though, the
 * customers endpoint 403s any other salon, so the list must follow the caller's scope:
 * one salon for an ADMIN or EMPLOYEE, every salon only for a SUPER_ADMIN.
 */
export const source: DataSource = "live";

export function useCustomers(scope: BusinessScope) {
	const businessId = scope.kind === "business" ? scope.businessId : null;
	// An unlinked ADMIN/EMPLOYEE has no salon to read; fanning out would only collect 403s.
	const enabled = scope.kind !== "unresolved";
	const query = useQuery({
		queryKey: queryKeys.customers.list(businessId),
		queryFn: ({ signal }) => listCustomers(businessId, { signal }),
		enabled,
	});
	return { ...query, isPending: enabled && query.isPending, data: query.data ?? [], source };
}

export function useCustomer(customerId: Id | null) {
	const { user } = useAuth();
	const { data, isPending, isError, error, refetch } = useCustomers(resolveBusinessScope(user));
	const customer = useMemo(
		() => data.find((c) => c.id === customerId) ?? null,
		[data, customerId],
	);
	return { data: customer, isPending, isError, error, refetch, source };
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
