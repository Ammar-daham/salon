import type { Id } from "@/lib/api/types";

export interface Customer {
	id: Id;
	firstName: string;
	lastName: string;
	email: string;
	phone: string;
	businessId: Id;
	businessName: string;
	tags: string[];
	notes: string | null;
	firstVisit: string;
	lastVisit: string;
	totalVisits: number;
	/** Lifetime spend in EUR. */
	totalSpend: number;
}

export function customerFullName(c: Pick<Customer, "firstName" | "lastName">) {
	return `${c.firstName} ${c.lastName}`;
}
