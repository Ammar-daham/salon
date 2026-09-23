"use client";

import { useMemo } from "react";
import type { Id, DataSource } from "@/lib/api/types";
import { useBusinesses } from "@/lib/resources/businesses/businesses.hooks";
import { generateEmployees } from "@/lib/mock/generators";
import type { Employee } from "./employees.types";

/**
 * Mock-backed. The `staff` table exists but its data access layer is entirely
 * stubbed and isn't registered as a Spring bean, so there is no endpoint to
 * read or write a roster.
 *
 * Creating staff, however, is genuinely live — POST /users is correctly scoped
 * server-side. That split is why this page shows a "Sample data" pill while its
 * create action still writes to the real database.
 *
 * Swapping this for the real thing means replacing the body of `useEmployees`
 * and flipping `source`. Nothing at the call sites changes.
 */
export const source: DataSource = "mock";

export function useEmployees(businessId: Id | null) {
	// Salons are real, so the mock roster is generated against actual business
	// ids and names — the shape of the platform is true even where the people
	// aren't.
	const { data: businesses, isPending, isError, error, refetch } = useBusinesses();

	const employees = useMemo<Employee[]>(() => {
		if (!businesses) return [];
		return businesses
			.filter((b) => businessId == null || b.id === businessId)
			.flatMap((b) => generateEmployees(b.id, b.name));
	}, [businesses, businessId]);

	return { data: employees, isPending, isError, error, refetch, source };
}

export function useEmployee(employeeId: Id | null) {
	const { data, isPending, isError, error, refetch } = useEmployees(null);
	const employee = useMemo(
		() => data.find((e) => e.id === employeeId) ?? null,
		[data, employeeId],
	);
	return { data: employee, isPending, isError, error, refetch, source };
}
