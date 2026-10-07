import type { BusinessHoursInput, EventInput } from "@fullcalendar/core";
import type { Id } from "@/lib/api/types";
import type { Appointment } from "@/lib/resources/appointments/appointments.types";
import type { DayOfWeek, WeeklyInterval } from "@/lib/resources/hours/hours.types";
import { addDays, timeOf } from "@/lib/utils/wallClock";

/**
 * The calendar runs in FullCalendar's "UTC" zone, so a Date's UTC fields are the salon's wall
 * clock: "2026-10-14T10:30" goes in as 10:30 and comes back out as 10:30, whatever the browser's zone.
 */

/** FullCalendar counts days from Sunday = 0. */
const FC_DAY: Record<DayOfWeek, number> = {
	SUNDAY: 0,
	MONDAY: 1,
	TUESDAY: 2,
	WEDNESDAY: 3,
	THURSDAY: 4,
	FRIDAY: 5,
	SATURDAY: 6,
};

/**
 * Opening hours or shifts as FullCalendar's business hours, so the rest of the week is shaded.
 * None at all shades every hour: the salon is closed, or the staff member has no shifts.
 * FullCalendar only shades around the definitions it is given, so an empty list would shade
 * nothing; one that is open on no day of the week shades it all.
 */
export function toBusinessHours(intervals: WeeklyInterval[]): BusinessHoursInput {
	if (intervals.length === 0) return [{ daysOfWeek: [] }];
	return intervals.map((i) => ({ daysOfWeek: [FC_DAY[i.dayOfWeek]], startTime: i.start, endTime: i.end }));
}

/**
 * The hours the grid shows, in whole hours: the salon's opening hours, stretched to fit any
 * appointment outside them. With no opening hours, a working day.
 */
export function visibleHours(
	intervals: WeeklyInterval[],
	appointments: Pick<Appointment, "startsAt" | "endsAt">[],
): { slotMinTime: string; slotMaxTime: string } {
	const starts = intervals.length > 0 ? intervals.map((i) => i.start) : ["08:00"];
	const ends = intervals.length > 0 ? intervals.map((i) => i.end) : ["20:00"];
	appointments.forEach((a) => {
		starts.push(timeOf(a.startsAt));
		// One that runs past midnight fills the rest of its first day.
		ends.push(a.endsAt.slice(0, 10) > a.startsAt.slice(0, 10) ? "24:00" : timeOf(a.endsAt));
	});

	const first = starts.reduce((min, t) => (t < min ? t : min));
	const last = ends.reduce((max, t) => (t > max ? t : max));
	const lastHour = Number(last.slice(0, 2)) + (last.endsWith(":00") ? 0 : 1);
	return { slotMinTime: `${first.slice(0, 2)}:00`, slotMaxTime: `${String(lastHour).padStart(2, "0")}:00` };
}

/** "yyyy-MM-ddTHH:mm" of a calendar Date. */
export function wallDateTime(date: Date): string {
	return date.toISOString().slice(0, 16);
}

/** The dates a view shows, both included; FullCalendar's end is the day after. */
export function dateRange(start: Date, end: Date): { from: string; to: string } {
	return { from: wallDateTime(start).slice(0, 10), to: addDays(wallDateTime(end).slice(0, 10), -1) };
}

/** Told apart from each other and from the status colours; used for staff without a calendar colour. */
const FALLBACK_COLOURS = ["#4F7CAC", "#5B8C5A", "#C7883A", "#8A6FB0", "#3E9C9C", "#B5566B", "#7A8B3C", "#6C7A89"];
const HEX_COLOUR = /^#[0-9a-f]{6}$/i;

/** A staff member's colour: their own calendar colour (DB-04), or one picked by id so it never moves. */
export function staffColour(staffId: Id, calendarColour: string | null | undefined): string {
	return calendarColour && HEX_COLOUR.test(calendarColour)
		? calendarColour
		: FALLBACK_COLOURS[staffId % FALLBACK_COLOURS.length];
}

/** An appointment as a calendar event, tinted with its staff member's colour. */
export function toEvent(appointment: Appointment, colour: string): EventInput {
	return {
		id: String(appointment.id),
		start: appointment.startsAt,
		end: appointment.endsAt,
		// Opaque, so overlapping events don't show through each other, and readable in both themes.
		backgroundColor: `color-mix(in srgb, ${colour} 18%, var(--surface-raised))`,
		borderColor: colour,
		textColor: "var(--ink)",
		// Cancelled and no-show ones didn't happen: still there, but faded.
		classNames: [
			"appointment-event",
			...(appointment.status === "CANCELLED" || appointment.status === "NO_SHOW" ? ["opacity-60"] : []),
		],
		extendedProps: { appointment },
	};
}
