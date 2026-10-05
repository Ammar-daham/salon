import type { Id } from "@/lib/api/types";

/**
 * A salon's own record of a client (DB-03) - not a user account. Visit counts,
 * spend and last-visit dates come from appointments, which don't exist yet.
 */
export interface Customer {
	id: Id;
	firstName: string;
	lastName: string;
	email: string | null;
	phone: string | null;
	notes: string | null;
	marketingConsent: boolean;
	/** Set once the client has an online account linked to this record; nothing links one yet. */
	userId: Id | null;
	businessId: Id;
	businessName: string;
	createdAt: string;
	updatedAt: string | null;
}

export function customerFullName(c: Pick<Customer, "firstName" | "lastName">) {
	return `${c.firstName} ${c.lastName}`;
}

/** Create and update share one shape: PUT replaces every field, so an omitted one is cleared. */
export interface CustomerInput {
	firstName: string;
	lastName: string;
	email: string | null;
	phone: string | null;
	notes: string | null;
	marketingConsent: boolean;
}
