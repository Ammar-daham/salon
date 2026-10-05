"use client";

import { useMemo } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/query/queryKeys";
import type { DataSource, Id } from "@/lib/api/types";
import { createEmployee, listEmployees, removeEmployee, updateEmployee } from "./employees.api";
import type { CreateEmployeeInput, Employee, UpdateEmployeeInput } from "./employees.types";

/**
 * Live: staff is a real, business-scoped resource now (BE-08/BE-35/DB-04).
 *
 * There is still no "get one employee" endpoint reachable from just an id - the
 * /employees/:id route carries no businessId - so useEmployee composes on top of
 * the same list query the roster view uses, exactly as the mock version did. That
 * also means the two share one cache entry: opening a detail page after visiting
 * the list is instant.
 */
export const source: DataSource = "live";

export function useEmployees(businessId: Id | null) {
	const query = useQuery({
		queryKey: queryKeys.employees.list(businessId),
		queryFn: ({ signal }) => listEmployees(businessId, { signal }),
	});
	return { ...query, data: query.data ?? [], source };
}

export function useEmployee(employeeId: Id | null) {
	const { data, isPending, isError, error, refetch } = useEmployees(null);
	const employee = useMemo(
		() => data.find((e) => e.id === employeeId) ?? null,
		[data, employeeId],
	);
	return { data: employee, isPending, isError, error, refetch, source };
}

function useEmployeeMutation<TVars, TResult>(mutationFn: (vars: TVars) => Promise<TResult>) {
	const qc = useQueryClient();
	return useMutation({
		mutationFn,
		onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.employees.all }),
	});
}

export function useCreateEmployee() {
	return useEmployeeMutation(
		({ businessId, input }: { businessId: Id; input: CreateEmployeeInput }) =>
			createEmployee(businessId, input),
	);
}

export function useUpdateEmployee() {
	return useEmployeeMutation(
		({ businessId, id, input }: { businessId: Id; id: Id; input: UpdateEmployeeInput }) =>
			updateEmployee(businessId, id, input),
	);
}

export function useDeleteEmployee() {
	return useEmployeeMutation(({ businessId, id }: { businessId: Id; id: Id }) =>
		removeEmployee(businessId, id),
	);
}
