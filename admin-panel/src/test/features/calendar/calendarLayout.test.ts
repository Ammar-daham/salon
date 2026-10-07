import { describe, expect, it } from "vitest";
import {
	dateRange,
	staffColour,
	toBusinessHours,
	toEvent,
	visibleHours,
	wallDateTime,
} from "@/features/calendar/calendarLayout";
import type { Appointment, AppointmentStatus } from "@/lib/resources/appointments/appointments.types";
import type { WeeklyInterval } from "@/lib/resources/hours/hours.types";

const week: WeeklyInterval[] = [
	{ dayOfWeek: "MONDAY", start: "09:00", end: "12:00" },
	{ dayOfWeek: "MONDAY", start: "13:00", end: "17:30" },
	{ dayOfWeek: "SATURDAY", start: "10:00", end: "14:00" },
	{ dayOfWeek: "SUNDAY", start: "11:00", end: "15:00" },
];

function appointment(status: AppointmentStatus = "BOOKED"): Appointment {
	return {
		id: 7,
		businessId: 1,
		businessName: "Glow Beauty Studio",
		currency: "EUR",
		timezone: "Europe/Berlin",
		customer: { id: 3, firstName: "Olivia", lastName: "Customer" },
		staff: { id: 2, firstName: "Mia", lastName: "Stylist" },
		service: { id: 5, name: "Classic Manicure" },
		startsAt: "2026-10-14T15:30",
		endsAt: "2026-10-14T16:15",
		status,
		price: 35.5,
		notes: null,
		createdAt: "2026-10-07T09:00:00Z",
		updatedAt: null,
	};
}

describe("toBusinessHours", () => {
	it("numbers the days from Sunday, as FullCalendar does", () => {
		expect(toBusinessHours(week)).toEqual([
			{ daysOfWeek: [1], startTime: "09:00", endTime: "12:00" },
			{ daysOfWeek: [1], startTime: "13:00", endTime: "17:30" },
			{ daysOfWeek: [6], startTime: "10:00", endTime: "14:00" },
			{ daysOfWeek: [0], startTime: "11:00", endTime: "15:00" },
		]);
	});

	it("shades every hour when there are none", () => {
		// Not [], which FullCalendar reads as nothing to shade around.
		expect(toBusinessHours([])).toEqual([{ daysOfWeek: [] }]);
	});
});

describe("visibleHours", () => {
	it("spans the opening hours in whole hours", () => {
		expect(visibleHours(week, [])).toEqual({ slotMinTime: "09:00", slotMaxTime: "18:00" });
	});

	it("stretches to fit an appointment outside them", () => {
		expect(
			visibleHours(week, [
				{ startsAt: "2026-10-12T07:45", endsAt: "2026-10-12T08:30" },
				{ startsAt: "2026-10-12T19:00", endsAt: "2026-10-12T20:00" },
			]),
		).toEqual({ slotMinTime: "07:00", slotMaxTime: "20:00" });
	});

	it("runs to midnight for one that ends the next day", () => {
		expect(visibleHours(week, [{ startsAt: "2026-10-12T23:30", endsAt: "2026-10-13T00:15" }]).slotMaxTime).toBe(
			"24:00",
		);
	});

	it("shows a working day when there are no opening hours", () => {
		expect(visibleHours([], [])).toEqual({ slotMinTime: "08:00", slotMaxTime: "20:00" });
	});
});

describe("dateRange", () => {
	it("reads the calendar's UTC dates as the salon's, and includes the last day", () => {
		expect(dateRange(new Date("2026-10-12T00:00:00Z"), new Date("2026-10-19T00:00:00Z"))).toEqual({
			from: "2026-10-12",
			to: "2026-10-18",
		});
		expect(wallDateTime(new Date("2026-10-14T10:30:00Z"))).toBe("2026-10-14T10:30");
	});
});

describe("staffColour", () => {
	it("uses the staff member's own calendar colour", () => {
		expect(staffColour(2, "#B76E79")).toBe("#B76E79");
	});

	it("picks one by id when they have none, or it isn't a hex colour", () => {
		expect(staffColour(2, null)).toBe(staffColour(2, undefined));
		expect(staffColour(2, "red; background: url(x)")).toBe(staffColour(2, null));
		expect(staffColour(2, null)).not.toBe(staffColour(3, null));
	});
});

describe("toEvent", () => {
	it("places the appointment at its wall-clock times, carrying it along", () => {
		const event = toEvent(appointment(), "#4F7CAC");
		expect(event).toMatchObject({ id: "7", start: "2026-10-14T15:30", end: "2026-10-14T16:15", borderColor: "#4F7CAC" });
		expect(event.extendedProps?.appointment.id).toBe(7);
	});

	it("fades the ones that didn't happen", () => {
		expect(toEvent(appointment("CONFIRMED"), "#4F7CAC").classNames).not.toContain("opacity-60");
		expect(toEvent(appointment("CANCELLED"), "#4F7CAC").classNames).toContain("opacity-60");
		expect(toEvent(appointment("NO_SHOW"), "#4F7CAC").classNames).toContain("opacity-60");
	});
});
