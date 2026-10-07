import { describe, expect, it } from "vitest";
import { appointmentHref, bookingHref, idParam } from "@/features/appointments/appointmentLinks";

describe("appointmentHref", () => {
	it("carries the salon, which a SUPER_ADMIN has no other way to know", () => {
		expect(appointmentHref({ id: 7, businessId: 1 })).toBe("/appointments/7?business=1");
		expect(appointmentHref({ id: 7, businessId: 1 }, "/edit")).toBe("/appointments/7/edit?business=1");
	});
});

describe("bookingHref", () => {
	it("opens the form empty", () => {
		expect(bookingHref()).toBe("/appointments/new");
	});

	it("starts the form with whoever the link is from", () => {
		expect(bookingHref({ businessId: 1, customerId: 3 })).toBe("/appointments/new?business=1&customer=3");
		expect(bookingHref({ businessId: 1, staffId: 2 })).toBe("/appointments/new?business=1&staff=2");
	});
});

describe("idParam", () => {
	it("reads a positive whole id", () => {
		expect(idParam("3")).toBe(3);
		expect(idParam(["3", "4"])).toBe(3);
	});

	it("is null for anything else", () => {
		expect(idParam(undefined)).toBeNull();
		expect(idParam("")).toBeNull();
		expect(idParam("abc")).toBeNull();
		expect(idParam("0")).toBeNull();
		expect(idParam("1.5")).toBeNull();
	});
});
