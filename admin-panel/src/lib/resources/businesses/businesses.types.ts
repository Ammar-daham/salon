import type { Id } from "@/lib/api/types";

export type BusinessStatus = "PENDING" | "APPROVED" | "REJECTED" | "SUSPENDED";

export const BUSINESS_STATUS_LABELS: Record<BusinessStatus, string> = {
	PENDING: "Pending",
	APPROVED: "Approved",
	REJECTED: "Rejected",
	SUSPENDED: "Suspended",
};

/** Status colour is a second channel only — the label always travels with it. */
export const BUSINESS_STATUS_TONE: Record<
	BusinessStatus,
	"success" | "warning" | "error" | "neutral"
> = {
	APPROVED: "success",
	PENDING: "warning",
	REJECTED: "error",
	SUSPENDED: "neutral",
};

export const BUSINESS_STATUSES: BusinessStatus[] = [
	"PENDING",
	"APPROVED",
	"REJECTED",
	"SUSPENDED",
];

export interface Address {
	id: Id;
	street: string;
	city: string;
	country: string;
	postalCode: string | null;
	latitude: string | null;
	longitude: string | null;
}

export interface Contact {
	id: Id;
	/** Free text on the backend — there is no contact-type enum. */
	type: string;
	value: string;
}

export interface SalonService {
	id: Id;
	name: string;
	description: string | null;
	durationMinutes: number;
	price: number;
	isActive: boolean;
}

export interface Business {
	id: Id;
	name: string;
	description: string | null;
	/** NOT NULL on the backend, and there is no upload endpoint — the client
	 *  supplies a URL or a data URI. */
	image: string;
	status: BusinessStatus;
	createdAt: string;
	updatedAt: string | null;
	addresses: Address[];
	contacts: Contact[];
	services: SalonService[];
}

export interface SalonServiceInput {
	name: string;
	description: string | null;
	durationMinutes: number;
	price: number;
	isActive: boolean;
}

export interface BusinessInput {
	name: string;
	description: string | null;
	image: string;
	/** Only meaningful on create. `addBusiness` defaults null to PENDING, and
	 *  that is the one moment status is writable from a plain create form. */
	status?: BusinessStatus;
}
