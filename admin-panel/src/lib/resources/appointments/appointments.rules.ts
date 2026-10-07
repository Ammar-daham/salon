import { wallClockNow } from "@/lib/utils/wallClock";
import type { Appointment, AppointmentStatus } from "./appointments.types";

/**
 * The backend's status rules, checked here so the panel only offers moves it would accept.
 * AppointmentStatus.canBecome and AppointmentService.changeStatus are the source of truth.
 */

/** Booked or confirmed: it hasn't ended one way or another, so it can still be moved or changed. */
export function isOpen(status: AppointmentStatus): boolean {
	return status === "BOOKED" || status === "CONFIRMED";
}

/** Where it can go from here, in the order the panel offers them. Confirming is optional. */
export function nextStatuses(status: AppointmentStatus): AppointmentStatus[] {
	switch (status) {
		case "BOOKED":
			return ["CONFIRMED", "COMPLETED", "NO_SHOW", "CANCELLED"];
		case "CONFIRMED":
			return ["COMPLETED", "NO_SHOW", "CANCELLED"];
		default:
			return [];
	}
}

/** Completed and no-show say how it went, so the backend only takes them once it has started. */
export function waitsForStart(status: AppointmentStatus): boolean {
	return status === "COMPLETED" || status === "NO_SHOW";
}

/** Whether it has started by the salon's clock. */
export function hasStarted(
	appointment: Pick<Appointment, "startsAt" | "timezone">,
	at: Date = new Date(),
): boolean {
	return appointment.startsAt <= wallClockNow(appointment.timezone, at);
}
