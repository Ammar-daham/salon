"use client";

import { useMemo } from "react";
import type { Id, DataSource } from "@/lib/api/types";
import { useBusinesses } from "@/lib/resources/businesses/businesses.hooks";
import { generateCustomers } from "@/lib/mock/generators";
import type { Customer } from "./customers.types";

/**
 * Mock-backed. Customers are just `users` rows with role CUSTOMER, and the
 * `business_customers` join table is an orphan — no Java model, no DAO, no
 * endpoint, and V2 never added its foreign keys. There is also no way to filter
 * GET /users by role or by salon server-side.
 *
 * Visit history and spend depend on appointments, which don't exist at all yet.
 */
export const source: DataSource = "mock";

export function useCustomers(businessId: Id | null) {
	const { data: businesses, isPending, isError, error, refetch } = useBusinesses();

	const customers = useMemo<Customer[]>(() => {
		if (!businesses) return [];
		return businesses
			.filter((b) => businessId == null || b.id === businessId)
			.flatMap((b) => generateCustomers(b.id, b.name));
	}, [businesses, businessId]);

	return { data: customers, isPending, isError, error, refetch, source };
}

export function useCustomer(customerId: Id | null) {
	const { data, isPending, isError, error, refetch } = useCustomers(null);
	const customer = useMemo(
		() => data.find((c) => c.id === customerId) ?? null,
		[data, customerId],
	);
	return { data: customer, isPending, isError, error, refetch, source };
}
