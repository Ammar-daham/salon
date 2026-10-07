import type { DayOfWeek, WeeklyHours, WeeklyInterval } from "./hours.types";

interface OpeningIntervalDto {
	day_of_week: DayOfWeek;
	opens_at: string;
	closes_at: string;
}

/** GET and PUT /businesses/{id}/hours both answer with this: the week as stored, in order. */
export interface BusinessHoursDto {
	timezone: string;
	hours: OpeningIntervalDto[];
}

interface WorkingIntervalDto {
	day_of_week: DayOfWeek;
	starts_at: string;
	ends_at: string;
}

/** GET and PUT /businesses/{id}/staff/{staffId}/schedule: the same shape, with shift names. */
export interface StaffScheduleDto {
	timezone: string;
	hours: WorkingIntervalDto[];
}

/**
 * Opening hours are opens_at/closes_at on the wire, a staff member's shifts
 * starts_at/ends_at. The panel uses start/end for both, so one editor serves both.
 */
export function toOpeningHours(dto: BusinessHoursDto): WeeklyHours {
	return {
		timezone: dto.timezone,
		intervals: dto.hours.map((h) => ({ dayOfWeek: h.day_of_week, start: h.opens_at, end: h.closes_at })),
	};
}

/** The PUT body: the whole week, replacing what is stored. A day left out is closed. */
export function toOpeningHoursRequest(intervals: WeeklyInterval[]): { hours: OpeningIntervalDto[] } {
	return {
		hours: intervals.map((i) => ({ day_of_week: i.dayOfWeek, opens_at: i.start, closes_at: i.end })),
	};
}

export function toWorkingHours(dto: StaffScheduleDto): WeeklyHours {
	return {
		timezone: dto.timezone,
		intervals: dto.hours.map((h) => ({ dayOfWeek: h.day_of_week, start: h.starts_at, end: h.ends_at })),
	};
}

/** The PUT body: the whole week, replacing what is stored. A day left out is a day off. */
export function toWorkingHoursRequest(intervals: WeeklyInterval[]): { hours: WorkingIntervalDto[] } {
	return {
		hours: intervals.map((i) => ({ day_of_week: i.dayOfWeek, starts_at: i.start, ends_at: i.end })),
	};
}
