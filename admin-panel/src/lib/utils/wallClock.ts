/**
 * Appointments and open slots are wall-clock times on the salon's clock, "yyyy-MM-ddTHH:mm"
 * (DB-09), not instants. They are shown as they are and never moved into the browser's zone:
 * 10:00 in Berlin reads 10:00 to a manager in London too. In this one format they also sort
 * and compare correctly as text.
 */

/** The date and time it is now on a zone's clock, "yyyy-MM-ddTHH:mm". */
export function wallClockNow(timeZone: string, at: Date = new Date()): string {
	const parts = new Intl.DateTimeFormat("en-US", {
		timeZone,
		year: "numeric",
		month: "2-digit",
		day: "2-digit",
		hour: "2-digit",
		minute: "2-digit",
		hourCycle: "h23",
	}).formatToParts(at);
	const part = (type: Intl.DateTimeFormatPartTypes) => parts.find((p) => p.type === type)?.value;
	return `${part("year")}-${part("month")}-${part("day")}T${part("hour")}:${part("minute")}`;
}

/** Today's date on a zone's clock, "yyyy-MM-dd". */
export function todayOn(timeZone: string, at: Date = new Date()): string {
	return wallClockNow(timeZone, at).slice(0, 10);
}

/** The browser's own zone, for a date filter that spans salons on different clocks. */
export function browserTimeZone(): string {
	return Intl.DateTimeFormat().resolvedOptions().timeZone;
}

/** A "yyyy-MM-dd" date some days later, or earlier for a negative count. */
export function addDays(date: string, days: number): string {
	const [year, month, day] = date.split("-").map(Number);
	return new Date(Date.UTC(year, month - 1, day + days)).toISOString().slice(0, 10);
}

/**
 * A "yyyy-MM-ddTHH:mm" some minutes later, by the clock's face. Only a preview: across a
 * daylight-saving change the backend, which adds the minutes to the instant, can end an hour apart.
 */
export function plusMinutes(dateTime: string, minutes: number): string {
	const [date, time] = dateTime.split("T");
	const [year, month, day] = date.split("-").map(Number);
	const [hours, mins] = time.split(":").map(Number);
	return new Date(Date.UTC(year, month - 1, day, hours, mins + minutes)).toISOString().slice(0, 16);
}

/** "14:30" of "2026-10-14T14:30". */
export function timeOf(dateTime: string): string {
	return dateTime.slice(11, 16);
}

const SHORT_DATE: Intl.DateTimeFormatOptions = { weekday: "short", day: "numeric", month: "short" };

/**
 * A wall-clock date (or the date of a date-time) in the reader's language, e.g. "Wed, Oct 14".
 * Read as a calendar date in UTC, so no zone can move it onto another day.
 */
export function formatWallDate(dateOrDateTime: string, options: Intl.DateTimeFormatOptions = SHORT_DATE): string {
	const [year, month, day] = dateOrDateTime.slice(0, 10).split("-").map(Number);
	return new Intl.DateTimeFormat(undefined, { ...options, timeZone: "UTC" }).format(
		new Date(Date.UTC(year, month - 1, day)),
	);
}
