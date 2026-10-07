import { describe, expect, it } from "vitest";
import {
	hasStarted,
	isOpen,
	nextStatuses,
	waitsForStart,
} from "@/lib/resources/appointments/appointments.rules";
import { APPOINTMENT_STATUSES } from "@/lib/resources/appointments/appointments.types";

describe("nextStatuses", () => {
	it("lets a booked appointment be confirmed or go straight to how it ended", () => {
		expect(nextStatuses("BOOKED")).toEqual(["CONFIRMED", "COMPLETED", "NO_SHOW", "CANCELLED"]);
	});

	it("never sends a confirmed appointment back to booked", () => {
		expect(nextStatuses("CONFIRMED")).toEqual(["COMPLETED", "NO_SHOW", "CANCELLED"]);
	});

	it("treats completed, cancelled and no-show as final", () => {
		expect(nextStatuses("COMPLETED")).toEqual([]);
		expect(nextStatuses("CANCELLED")).toEqual([]);
		expect(nextStatuses("NO_SHOW")).toEqual([]);
	});

	it("never offers the status it already has", () => {
		APPOINTMENT_STATUSES.forEach((status) => expect(nextStatuses(status)).not.toContain(status));
	});
});

describe("isOpen", () => {
	it("is booked or confirmed only", () => {
		expect(APPOINTMENT_STATUSES.filter(isOpen)).toEqual(["BOOKED", "CONFIRMED"]);
	});
});

describe("waitsForStart", () => {
	it("holds back completed and no-show, which say how it went", () => {
		expect(APPOINTMENT_STATUSES.filter(waitsForStart)).toEqual(["COMPLETED", "NO_SHOW"]);
	});
});

describe("hasStarted", () => {
	const berlin = { startsAt: "2026-10-14T10:30", timezone: "Europe/Berlin" };

	it("compares the start with now on the salon's clock", () => {
		expect(hasStarted(berlin, new Date("2026-10-14T08:29:00Z"))).toBe(false);
		expect(hasStarted(berlin, new Date("2026-10-14T08:30:00Z"))).toBe(true);
	});

	it("doesn't read the start in the browser's zone", () => {
		// 10:30 in New York is 14:30 UTC, long after 10:30 in Berlin.
		const newYork = { startsAt: "2026-10-14T10:30", timezone: "America/New_York" };
		expect(hasStarted(newYork, new Date("2026-10-14T12:00:00Z"))).toBe(false);
		expect(hasStarted(newYork, new Date("2026-10-14T14:30:00Z"))).toBe(true);
	});
});
