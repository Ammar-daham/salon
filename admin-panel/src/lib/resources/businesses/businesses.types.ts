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

/** A ?status= value as a status, or undefined for anything that isn't one. */
export function businessStatusParam(value: string | string[] | undefined): BusinessStatus | undefined {
	return typeof value === "string" && (BUSINESS_STATUSES as string[]).includes(value)
		? (value as BusinessStatus)
		: undefined;
}

/** The salon list, filtered to one status or showing them all. */
export function businessListHref(status?: BusinessStatus): string {
	return status ? `/businesses?status=${status}` : "/businesses";
}

export interface Address {
	id: Id;
	street: string;
	city: string;
	country: string;
	postalCode: string | null;
	/** NUMERIC(9,6) on the backend, so a JSON number (DB-07). Both set or both null. */
	latitude: number | null;
	longitude: number | null;
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
	/** ISO 4217 code every price at this salon is in, e.g. "EUR" (DB-08). */
	currency: string;
	/** IANA zone the salon's hours and appointments are on, e.g. "Europe/Berlin" (DB-09). */
	timezone: string;
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
	/** Not sent: a new salon is PENDING, and only PUT /businesses/{id}/status moves it. */
	status?: BusinessStatus;
	/** Required: the update replaces it, like every other field. */
	currency: string;
	/** Required: the update replaces it, like every other field. */
	timezone: string;
}
