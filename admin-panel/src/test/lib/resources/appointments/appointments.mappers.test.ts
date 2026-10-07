import { describe, expect, it } from "vitest";
import {
	appointmentInputFrom,
	salonOf,
	toAppointment,
	toAppointmentQuery,
	toAppointmentRequest,
	toAvailability,
	type AppointmentDto,
	type SalonRef,
} from "@/lib/resources/appointments/appointments.mappers";

const salon: SalonRef = { id: 1, name: "Glow Beauty Studio", currency: "EUR", timezone: "Europe/Berlin" };

const dto: AppointmentDto = {
	id: 7,
	customer: { id: 3, first_name: "Olivia", last_name: "Customer" },
	staff: { id: 2, first_name: "Mia", last_name: "Stylist" },
	service: { id: 5, name: "Classic Manicure" },
	starts_at: "2026-10-14T15:30",
	ends_at: "2026-10-14T16:15",
	status: "BOOKED",
	price: 35.5,
	notes: "First visit.",
	created_at: "2026-10-07T09:00:00Z",
	updated_at: null,
};

describe("toAppointment", () => {
	it("maps the DTO plus the caller-supplied salon, keeping times on the salon's clock", () => {
		expect(toAppointment(dto, salon)).toEqual({
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
			status: "BOOKED",
			price: 35.5,
			notes: "First visit.",
			createdAt: "2026-10-07T09:00:00Z",
			updatedAt: null,
		});
	});

	it("gives the salon back for the next write", () => {
		expect(salonOf(toAppointment(dto, salon))).toEqual(salon);
	});
});

describe("toAppointmentRequest", () => {
	it("sends snake_case ids and the start on the salon's clock, but no end or price", () => {
		expect(
			toAppointmentRequest({
				customerId: 3,
				staffId: 2,
				serviceId: 5,
				startsAt: "2026-10-14T15:30",
				notes: "  Bring the gel colours. ",
			}),
		).toEqual({
			customer_id: 3,
			staff_id: 2,
			service_id: 5,
			starts_at: "2026-10-14T15:30",
			notes: "Bring the gel colours.",
		});
	});

	it("sends blank notes as null, so a PUT clears them", () => {
		const body = toAppointmentRequest({ customerId: 3, staffId: 2, serviceId: 5, startsAt: "2026-10-14T15:30", notes: "  " });
		expect(body.notes).toBeNull();
	});
});

describe("appointmentInputFrom", () => {
	it("carries every current field into the full-replace body, overriding only what changed", () => {
		expect(appointmentInputFrom(toAppointment(dto, salon), { startsAt: "2026-10-15T10:00" })).toEqual({
			customerId: 3,
			staffId: 2,
			serviceId: 5,
			startsAt: "2026-10-15T10:00",
			notes: "First visit.",
		});
	});
});

describe("toAppointmentQuery", () => {
	it("names the filters as the backend does", () => {
		expect(toAppointmentQuery({ from: "2026-10-14", to: "2026-10-20", staffId: 2, customerId: 3 })).toEqual({
			from: "2026-10-14",
			to: "2026-10-20",
			staff_id: 2,
			customer_id: 3,
		});
	});

	it("leaves out a filter that isn't set, which means any", () => {
		expect(toAppointmentQuery({ customerId: 3 })).toEqual({ customer_id: 3 });
		expect(toAppointmentQuery({})).toEqual({});
	});
});

describe("toAvailability", () => {
	it("maps each performer's slots", () => {
		expect(
			toAvailability({
				timezone: "Europe/Berlin",
				duration_minutes: 45,
				staff: [
					{
						id: 2,
						first_name: "Mia",
						last_name: "Stylist",
						slots: [{ starts_at: "2026-10-14T09:00", ends_at: "2026-10-14T09:45" }],
					},
					{ id: 4, first_name: "Noah", last_name: "Colourist", slots: [] },
				],
			}),
		).toEqual({
			timezone: "Europe/Berlin",
			durationMinutes: 45,
			staff: [
				{
					id: 2,
					firstName: "Mia",
					lastName: "Stylist",
					slots: [{ startsAt: "2026-10-14T09:00", endsAt: "2026-10-14T09:45" }],
				},
				{ id: 4, firstName: "Noah", lastName: "Colourist", slots: [] },
			],
		});
	});
});
