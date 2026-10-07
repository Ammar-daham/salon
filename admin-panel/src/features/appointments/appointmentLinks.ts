import type { Id } from "@/lib/api/types";
import type { Appointment } from "@/lib/resources/appointments/appointments.types";

/**
 * The salon travels in the query string: appointments are read per salon, and a SUPER_ADMIN
 * has none of their own. A salon's own staff always read their own, whatever the link says.
 */
export function appointmentHref(appointment: Pick<Appointment, "id" | "businessId">, page: "" | "/edit" = "") {
	return `/appointments/${appointment.id}${page}?business=${appointment.businessId}`;
}

/** "3" from ?business=3, or null when it is missing or not an id. */
export function idParam(value: string | string[] | undefined): Id | null {
	const id = Number(Array.isArray(value) ? value[0] : value);
	return value != null && Number.isInteger(id) && id > 0 ? id : null;
}
