import { describe, expect, it } from "vitest";
import type { WeeklyInterval } from "@/lib/resources/hours/hours.types";
import { addMinutes, weekErrors } from "@/lib/resources/hours/hours.validation";

const monday = (start: string, end: string): WeeklyInterval => ({ dayOfWeek: "MONDAY", start, end });

describe("weekErrors", () => {
	it("accepts a week whose intervals touch but don't overlap", () => {
		expect(weekErrors([monday("09:00", "12:00"), monday("12:00", "18:00")])).toEqual([null, null]);
	});

	it("flags an interval that doesn't end after it starts, or is missing a time", () => {
		expect(weekErrors([monday("17:00", "09:00"), monday("10:00", "10:00"), monday("", "12:00")])).toEqual([
			"Must end after it starts.",
			"Must end after it starts.",
			"Enter a start and an end time.",
		]);
	});

	it("flags both intervals of an overlap, naming the other one", () => {
		expect(weekErrors([monday("09:00", "13:00"), monday("12:30", "17:00")])).toEqual([
			"Overlaps 12:30–17:00.",
			"Overlaps 09:00–13:00.",
		]);
	});

	it("only compares intervals on the same day", () => {
		expect(weekErrors([monday("09:00", "17:00"), { dayOfWeek: "TUESDAY", start: "09:00", end: "17:00" }])).toEqual([
			null,
			null,
		]);
	});

	it("doesn't report an overlap with an interval that is already wrong", () => {
		expect(weekErrors([monday("09:00", "17:00"), monday("18:00", "10:00")])).toEqual([
			null,
			"Must end after it starts.",
		]);
	});
});

describe("addMinutes", () => {
	it("adds across the hour", () => {
		expect(addMinutes("09:45", 30)).toBe("10:15");
	});

	it("stops at 23:59, since an interval can't run past midnight", () => {
		expect(addMinutes("23:30", 60)).toBe("23:59");
	});
});
