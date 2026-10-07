import { describe, expect, it } from "vitest";
import {
	addDays,
	formatWallDate,
	plusMinutes,
	timeOf,
	todayOn,
	wallClockNow,
} from "@/lib/utils/wallClock";

describe("wallClockNow", () => {
	it("reads the time on the zone's clock, daylight saving included", () => {
		// CEST is UTC+2 in October.
		expect(wallClockNow("Europe/Berlin", new Date("2026-10-14T08:30:00Z"))).toBe("2026-10-14T10:30");
	});

	it("can be on another day than UTC", () => {
		// EDT is UTC-4.
		expect(wallClockNow("America/New_York", new Date("2026-10-14T02:00:00Z"))).toBe("2026-10-13T22:00");
	});

	it("writes the hour after midnight as 00, never 24", () => {
		expect(wallClockNow("UTC", new Date("2026-10-14T00:05:00Z"))).toBe("2026-10-14T00:05");
	});
});

describe("todayOn", () => {
	it("is the date on the zone's clock", () => {
		expect(todayOn("Asia/Tokyo", new Date("2026-10-14T20:00:00Z"))).toBe("2026-10-15");
	});
});

describe("addDays", () => {
	it("crosses months and years", () => {
		expect(addDays("2026-12-30", 3)).toBe("2027-01-02");
		expect(addDays("2026-03-01", -1)).toBe("2026-02-28");
	});

	it("counts calendar days, whatever daylight saving does that night", () => {
		expect(addDays("2026-10-24", 1)).toBe("2026-10-25");
		expect(addDays("2026-10-25", 1)).toBe("2026-10-26");
	});
});

describe("plusMinutes", () => {
	it("moves past midnight onto the next day", () => {
		expect(plusMinutes("2026-10-14T23:30", 45)).toBe("2026-10-15T00:15");
	});
});

describe("timeOf", () => {
	it("is the HH:mm of a date-time", () => {
		expect(timeOf("2026-10-14T09:05")).toBe("09:05");
	});
});

describe("formatWallDate", () => {
	it("keeps the calendar date, whichever zone the browser is in", () => {
		expect(formatWallDate("2026-10-14T23:45", { day: "numeric" })).toBe("14");
		expect(formatWallDate("2026-10-14", { day: "numeric" })).toBe("14");
	});
});
