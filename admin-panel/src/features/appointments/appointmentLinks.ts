import type { Id } from "@/lib/api/types";
import type { Appointment } from "@/lib/resources/appointments/appointments.types";

/**
 * The salon travels in the query string: appointments are read per salon, and a SUPER_ADMIN
 * has none of their own. A salon's own staff always read their own, whatever the link says.
 */
export function appointmentHref(appointment: Pick<Appointment, "id" | "businessId">, page: "" | "/edit" = "") {
	return `/appointments/${appointment.id}${page}?business=${appointment.businessId}`;
}

/** The booking form, starting with a salon, client or staff member when booking from their page. */
export function bookingHref(start: { businessId?: Id; customerId?: Id; staffId?: Id } = {}) {
	const query = new URLSearchParams();
	if (start.businessId != null) query.set("business", String(start.businessId));
	if (start.customerId != null) query.set("customer", String(start.customerId));
	if (start.staffId != null) query.set("staff", String(start.staffId));
	const search = query.toString();
	return search ? `/appointments/new?${search}` : "/appointments/new";
}

/** "3" from ?business=3, or null when it is missing or not an id. */
export function idParam(value: string | string[] | undefined): Id | null {
	const id = Number(Array.isArray(value) ? value[0] : value);
	return value != null && Number.isInteger(id) && id > 0 ? id : null;
}
