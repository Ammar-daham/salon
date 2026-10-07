import { hasStarted, isOpen } from "@/lib/resources/appointments/appointments.rules";
import type { Appointment } from "@/lib/resources/appointments/appointments.types";

export interface VisitSummary {
	/** Completed appointments: a cancelled or no-show one isn't a visit. */
	visits: number;
	/** What the completed ones cost when booked, in `currency`; null with no visits. */
	spent: number | null;
	currency: string | null;
	/** "yyyy-MM-ddTHH:mm" of the latest completed one, on the salon's clock. */
	lastVisit: string | null;
	/** The soonest booked or confirmed one that hasn't started yet. */
	next: Appointment | null;
}

/** A client's visits, from their appointment history. A client belongs to one salon, so one currency. */
export function visitSummary(appointments: Appointment[], at: Date = new Date()): VisitSummary {
	const completed = appointments.filter((a) => a.status === "COMPLETED");
	const upcoming = appointments.filter((a) => isOpen(a.status) && !hasStarted(a, at));

	return {
		visits: completed.length,
		spent: completed.length > 0 ? completed.reduce((sum, a) => sum + a.price, 0) : null,
		currency: completed[0]?.currency ?? null,
		lastVisit: completed.reduce<string | null>(
			(latest, a) => (latest && latest > a.startsAt ? latest : a.startsAt),
			null,
		),
		next: upcoming.reduce<Appointment | null>(
			(soonest, a) => (soonest && soonest.startsAt < a.startsAt ? soonest : a),
			null,
		),
	};
}
