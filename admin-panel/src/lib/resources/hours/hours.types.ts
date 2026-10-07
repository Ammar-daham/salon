/**
 * ISO weekday names, exactly as the backend sends them, so nothing here depends on
 * JavaScript's Sunday = 0.
 */
export type DayOfWeek =
	| "MONDAY"
	| "TUESDAY"
	| "WEDNESDAY"
	| "THURSDAY"
	| "FRIDAY"
	| "SATURDAY"
	| "SUNDAY";

export const DAYS_OF_WEEK: DayOfWeek[] = [
	"MONDAY",
	"TUESDAY",
	"WEDNESDAY",
	"THURSDAY",
	"FRIDAY",
	"SATURDAY",
	"SUNDAY",
];

export const DAY_LABELS: Record<DayOfWeek, string> = {
	MONDAY: "Monday",
	TUESDAY: "Tuesday",
	WEDNESDAY: "Wednesday",
	THURSDAY: "Thursday",
	FRIDAY: "Friday",
	SATURDAY: "Saturday",
	SUNDAY: "Sunday",
};

/**
 * One stretch of a weekly schedule: a salon's opening hours or a staff member's shift.
 * Times are "HH:mm" on the salon's clock, and an interval can't run past midnight.
 */
export interface WeeklyInterval {
	dayOfWeek: DayOfWeek;
	start: string;
	end: string;
}

/**
 * A whole week, as the backend stores and returns it. A day with no interval is closed
 * (or a day off); two intervals on one day are e.g. a lunch break.
 */
export interface WeeklyHours {
	/** The salon's IANA time zone, e.g. "Europe/Berlin": the clock the times are on. */
	timezone: string;
	intervals: WeeklyInterval[];
}
