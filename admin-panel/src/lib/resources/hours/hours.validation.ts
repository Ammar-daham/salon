import type { WeeklyInterval } from "./hours.types";

/**
 * The backend's rules for a week, checked before saving so the editor can point at the
 * interval that breaks one: each ends after it starts, and a day's intervals may touch
 * (12:00-13:00, 13:00-17:00) but not overlap. One message, or null, per interval, in the
 * order given. "HH:mm" strings compare correctly as text.
 */
export function weekErrors(intervals: WeeklyInterval[]): (string | null)[] {
	const own: (string | null)[] = intervals.map((interval) => {
		if (!interval.start || !interval.end) return "Enter a start and an end time.";
		if (interval.start >= interval.end) return "Must end after it starts.";
		return null;
	});

	// Overlaps are judged between intervals that are fine on their own, so both sides of one are flagged.
	return intervals.map((a, i) => {
		if (own[i]) return own[i];
		const clash = intervals.find(
			(b, j) =>
				j !== i && !own[j] && b.dayOfWeek === a.dayOfWeek && a.start < b.end && b.start < a.end,
		);
		return clash ? `Overlaps ${clash.start}–${clash.end}.` : null;
	});
}

/** "HH:mm" plus some minutes, held at 23:59 so an interval never runs past midnight. */
export function addMinutes(time: string, minutes: number): string {
	const [hours, mins] = time.split(":").map(Number);
	const total = Math.min(hours * 60 + mins + minutes, 23 * 60 + 59);
	return `${String(Math.floor(total / 60)).padStart(2, "0")}:${String(total % 60).padStart(2, "0")}`;
}
