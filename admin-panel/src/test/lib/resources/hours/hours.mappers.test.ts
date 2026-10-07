import { describe, expect, it } from "vitest";
import {
	toOpeningHours,
	toOpeningHoursRequest,
	toWorkingHours,
	toWorkingHoursRequest,
	type BusinessHoursDto,
	type StaffScheduleDto,
} from "@/lib/resources/hours/hours.mappers";

const dto: BusinessHoursDto = {
	timezone: "Europe/Berlin",
	hours: [
		{ day_of_week: "TUESDAY", opens_at: "09:00", closes_at: "12:00" },
		{ day_of_week: "TUESDAY", opens_at: "13:00", closes_at: "18:00" },
	],
};

describe("toOpeningHours", () => {
	it("keeps the time zone and reads opens_at/closes_at as start/end", () => {
		expect(toOpeningHours(dto)).toEqual({
			timezone: "Europe/Berlin",
			intervals: [
				{ dayOfWeek: "TUESDAY", start: "09:00", end: "12:00" },
				{ dayOfWeek: "TUESDAY", start: "13:00", end: "18:00" },
			],
		});
	});
});

describe("toOpeningHoursRequest", () => {
	it("sends the whole week back in the wire's own names", () => {
		expect(toOpeningHoursRequest(toOpeningHours(dto).intervals)).toEqual({ hours: dto.hours });
	});

	it("sends an empty week, which closes every day", () => {
		expect(toOpeningHoursRequest([])).toEqual({ hours: [] });
	});
});

const schedule: StaffScheduleDto = {
	timezone: "Europe/Berlin",
	hours: [{ day_of_week: "WEDNESDAY", starts_at: "09:00", ends_at: "17:00" }],
};

describe("toWorkingHours", () => {
	it("reads a shift's starts_at/ends_at into the same start/end as opening hours", () => {
		expect(toWorkingHours(schedule)).toEqual({
			timezone: "Europe/Berlin",
			intervals: [{ dayOfWeek: "WEDNESDAY", start: "09:00", end: "17:00" }],
		});
	});
});

describe("toWorkingHoursRequest", () => {
	it("sends shifts as starts_at/ends_at, not the salon's opens_at/closes_at", () => {
		expect(toWorkingHoursRequest(toWorkingHours(schedule).intervals)).toEqual({ hours: schedule.hours });
	});
});
