import type { Id } from "@/lib/api/types";

/**
 * Where an appointment is in BOOKED → CONFIRMED → COMPLETED / CANCELLED / NO_SHOW (DB-14).
 * Confirming is optional, and the last three are final.
 */
export type AppointmentStatus = "BOOKED" | "CONFIRMED" | "COMPLETED" | "CANCELLED" | "NO_SHOW";

export const APPOINTMENT_STATUSES: AppointmentStatus[] = [
	"BOOKED",
	"CONFIRMED",
	"COMPLETED",
	"CANCELLED",
	"NO_SHOW",
];

export const APPOINTMENT_STATUS_LABELS: Record<AppointmentStatus, string> = {
	BOOKED: "Booked",
	CONFIRMED: "Confirmed",
	COMPLETED: "Completed",
	CANCELLED: "Cancelled",
	NO_SHOW: "No-show",
};

/** Status colour is a second channel only — the label always travels with it. */
export const APPOINTMENT_STATUS_TONE: Record<
	AppointmentStatus,
	"success" | "warning" | "info" | "neutral" | "primary"
> = {
	BOOKED: "info",
	CONFIRMED: "primary",
	COMPLETED: "success",
	CANCELLED: "neutral",
	NO_SHOW: "warning",
};

/** Who an appointment is for or with. Still named once removed (DB-13), so history stays readable. */
export interface PersonRef {
	id: Id;
	firstName: string;
	lastName: string;
}

export function personName(person: Pick<PersonRef, "firstName" | "lastName">) {
	return `${person.firstName} ${person.lastName}`;
}

/** One service by one staff member for one client. */
export interface Appointment {
	id: Id;
	businessId: Id;
	businessName: string;
	/** The salon's ISO 4217 code, which `price` is in. */
	currency: string;
	/** The salon's IANA zone: the clock `startsAt` and `endsAt` are on. */
	timezone: string;
	customer: PersonRef;
	staff: PersonRef;
	service: { id: Id; name: string };
	/** "yyyy-MM-ddTHH:mm" on the salon's clock. */
	startsAt: string;
	/** Set by the server from the service's duration. */
	endsAt: string;
	status: AppointmentStatus;
	/** What the service cost when it was booked. */
	price: number;
	notes: string | null;
	createdAt: string;
	updatedAt: string | null;
}

/**
 * Book and reschedule share one body, and PUT replaces every field. The service sets how
 * long the appointment lasts and what it costs.
 */
export interface AppointmentInput {
	customerId: Id;
	staffId: Id;
	serviceId: Id;
	/** "yyyy-MM-ddTHH:mm" on the salon's clock. */
	startsAt: string;
	notes: string | null;
}

/** Narrows a list on the server. Dates are "yyyy-MM-dd" on the salon's clock, both included. */
export interface AppointmentFilter {
	from?: string;
	to?: string;
	staffId?: Id;
	customerId?: Id;
}

/** A time a service could start, on the salon's clock. */
export interface Slot {
	startsAt: string;
	endsAt: string;
}

export interface StaffSlots extends PersonRef {
	slots: Slot[];
}

/**
 * When a service can be booked, per active staff member who performs it: inside opening hours
 * and their shift, clear of time off and other appointments, on a quarter-hour grid. It is
 * advice, not a rule: booking only refuses a double booking.
 */
export interface Availability {
	timezone: string;
	durationMinutes: number;
	staff: StaffSlots[];
}
