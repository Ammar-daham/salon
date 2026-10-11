import type { Id } from "@/lib/api/types";
import type { AppointmentFilter } from "@/lib/resources/appointments/appointments.types";
import type { BusinessPageQuery } from "@/lib/resources/businesses/businesses.api";
import type { CustomerPageQuery } from "@/lib/resources/customers/customers.api";
import type { UserPageQuery } from "@/lib/resources/users/users.api";

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
		// Every list of salons, so an edit refreshes the pickers and each cached page alike.
		list: () => [...queryKeys.businesses.all, "list"] as const,
		listAll: () => [...queryKeys.businesses.list(), "all"] as const,
		// The query is part of the key, so each page, search and filter is cached on its own.
		page: (query: BusinessPageQuery) => [...queryKeys.businesses.list(), "page", query] as const,
		detail: (id: Id) => [...queryKeys.businesses.all, "detail", id] as const,
		services: (businessId: Id) =>
			[...queryKeys.businesses.detail(businessId), "services"] as const,
	},
	hours: {
		all: ["hours"] as const,
		// The PUT answers with the stored week, so a save writes it straight into this entry.
		business: (businessId: Id) => [...queryKeys.hours.all, "business", businessId] as const,
		staff: (staffId: Id) => [...queryKeys.hours.all, "staff", staffId] as const,
	},
	users: {
		all: ["users"] as const,
		list: () => [...queryKeys.users.all, "list"] as const,
		page: (query: UserPageQuery) => [...queryKeys.users.list(), query] as const,
		detail: (id: Id) => [...queryKeys.users.all, "detail", id] as const,
	},
	appointments: {
		all: ["appointments"] as const,
		// null = every salon (platform scope). The filter is part of the key, so each date range,
		// staff member and client is cached on its own and a booking invalidates them all.
		lists: () => [...queryKeys.appointments.all, "list"] as const,
		list: (businessId: Id | null, filter: AppointmentFilter) =>
			[...queryKeys.appointments.lists(), businessId, filter] as const,
		detail: (id: Id) => [...queryKeys.appointments.all, "detail", id] as const,
		// Open slots hang off appointments because every booking, move and cancellation changes them.
		availabilities: () => [...queryKeys.appointments.all, "availability"] as const,
		availability: (businessId: Id, serviceId: Id, date: string) =>
			[...queryKeys.appointments.availabilities(), businessId, serviceId, date] as const,
	},
	customers: {
		all: ["customers"] as const,
		// The query names the salon, or none for every salon (platform scope).
		page: (query: CustomerPageQuery) => [...queryKeys.customers.all, "list", query] as const,
		detail: (id: Id) => [...queryKeys.customers.all, "detail", id] as const,
	},
	employees: {
		all: ["employees"] as const,
		// null/omitted = every business (platform scope); a staff roster is
		// fetched per business, so the two cases need distinct keys.
		list: (businessId?: Id | null) =>
			businessId == null
				? ([...queryKeys.employees.all, "list"] as const)
				: ([...queryKeys.employees.all, "list", businessId] as const),
		detail: (id: Id) => [...queryKeys.employees.all, "detail", id] as const,
	},
	stats: {
		all: ["stats"] as const,
		overview: () => [...queryKeys.stats.all, "overview"] as const,
	},
} as const;
