import { describe, expect, it } from "vitest";
import { visitSummary } from "@/features/customers/customerVisits";
import type { Appointment, AppointmentStatus } from "@/lib/resources/appointments/appointments.types";

function appointment(id: number, startsAt: string, status: AppointmentStatus, price = 30): Appointment {
	return {
		id,
		businessId: 1,
		businessName: "Glow Beauty Studio",
		currency: "SEK",
		timezone: "Europe/Stockholm",
		customer: { id: 3, firstName: "Olivia", lastName: "Customer" },
		staff: { id: 2, firstName: "Mia", lastName: "Stylist" },
		service: { id: 5, name: "Classic Manicure" },
		startsAt,
		endsAt: startsAt,
		status,
		price,
		notes: null,
		createdAt: "2026-09-01T09:00:00Z",
		updatedAt: null,
	};
}

// 12:00 on 2026-10-07 in Stockholm.
const now = new Date("2026-10-07T10:00:00Z");

describe("visitSummary", () => {
	it("counts only completed appointments as visits, at the price each was booked at", () => {
		const summary = visitSummary(
			[
				appointment(1, "2026-09-02T10:00", "COMPLETED", 35.5),
				appointment(2, "2026-09-20T14:00", "COMPLETED", 40),
				appointment(3, "2026-09-25T09:00", "CANCELLED", 99),
				appointment(4, "2026-09-28T09:00", "NO_SHOW", 99),
			],
			now,
		);
		expect(summary).toMatchObject({ visits: 2, spent: 75.5, currency: "SEK", lastVisit: "2026-09-20T14:00" });
	});

	it("has nothing to say about a client with no visits", () => {
		expect(visitSummary([appointment(1, "2026-09-25T09:00", "CANCELLED")], now)).toEqual({
			visits: 0,
			spent: null,
			currency: null,
			lastVisit: null,
			next: null,
		});
	});

	it("picks the soonest open appointment still to start, on the salon's clock", () => {
		const summary = visitSummary(
			[
				appointment(1, "2026-10-20T10:00", "BOOKED"),
				appointment(2, "2026-10-09T10:00", "CONFIRMED"),
				appointment(3, "2026-10-08T10:00", "CANCELLED"),
				// Started at 11:00 Stockholm time, an hour before now.
				appointment(4, "2026-10-07T11:00", "BOOKED"),
			],
			now,
		);
		expect(summary.next?.id).toBe(2);
	});
});
