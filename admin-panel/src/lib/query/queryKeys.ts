import type { Id } from "@/lib/api/types";

/**
 * Typed key factory. Never inline a key array at a call site — invalidation
 * correctness depends on list and detail keys sharing a prefix.
 *
 * Note `services` hangs off the *business* detail key rather than having a root
 * of its own: the backend has no services list endpoint, so a service list is a
 * projection of the business response. Creating a service must therefore
 * invalidate the business, not a phantom services list.
 */
export const queryKeys = {
	businesses: {
		all: ["businesses"] as const,
		list: () => [...queryKeys.businesses.all, "list"] as const,
		detail: (id: Id) => [...queryKeys.businesses.all, "detail", id] as const,
		services: (businessId: Id) =>
			[...queryKeys.businesses.detail(businessId), "services"] as const,
	},
	users: {
		all: ["users"] as const,
		list: () => [...queryKeys.users.all, "list"] as const,
		detail: (id: Id) => [...queryKeys.users.all, "detail", id] as const,
	},
	appointments: {
		all: ["appointments"] as const,
		list: () => [...queryKeys.appointments.all, "list"] as const,
		detail: (id: Id) => [...queryKeys.appointments.all, "detail", id] as const,
	},
	customers: {
		all: ["customers"] as const,
		list: () => [...queryKeys.customers.all, "list"] as const,
		detail: (id: Id) => [...queryKeys.customers.all, "detail", id] as const,
	},
	employees: {
		all: ["employees"] as const,
		list: () => [...queryKeys.employees.all, "list"] as const,
		detail: (id: Id) => [...queryKeys.employees.all, "detail", id] as const,
	},
	stats: {
		all: ["stats"] as const,
		overview: () => [...queryKeys.stats.all, "overview"] as const,
	},
} as const;
