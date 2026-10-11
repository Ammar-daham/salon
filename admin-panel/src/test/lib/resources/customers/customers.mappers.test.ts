import { describe, expect, it } from "vitest";
import {
	customerInputFrom,
	toCustomer,
	toCustomerRequest,
	type CustomerDto,
} from "@/lib/resources/customers/customers.mappers";

const dto: CustomerDto = {
	id: 1,
	business_id: 1,
	business_name: "Glow Beauty Studio",
	user_id: null,
	first_name: "Olivia",
	last_name: "Client",
	email: "olivia@example.test",
	phone: "+49 30 7770001",
	notes: "Prefers mornings.",
	marketing_consent: true,
	created_at: "2026-10-01T09:00:00Z",
	updated_at: null,
};

describe("toCustomer", () => {
	it("maps the DTO, with the salon it belongs to", () => {
		expect(toCustomer(dto)).toEqual({
			id: 1,
			firstName: "Olivia",
			lastName: "Client",
			email: "olivia@example.test",
			phone: "+49 30 7770001",
			notes: "Prefers mornings.",
			marketingConsent: true,
			userId: null,
			businessId: 1,
			businessName: "Glow Beauty Studio",
			createdAt: "2026-10-01T09:00:00Z",
			updatedAt: null,
		});
	});
});

describe("toCustomerRequest", () => {
	it("sends snake_case with trimmed names", () => {
		expect(
			toCustomerRequest({
				firstName: "  Ella ",
				lastName: "Walk-in",
				email: "ella@example.test",
				phone: "+49 30 7770009",
				notes: "Allergic to latex.",
				marketingConsent: false,
			}),
		).toEqual({
			first_name: "Ella",
			last_name: "Walk-in",
			email: "ella@example.test",
			phone: "+49 30 7770009",
			notes: "Allergic to latex.",
			marketing_consent: false,
		});
	});

	it("sends blank optional fields as null, so a PUT clears them", () => {
		const body = toCustomerRequest({
			firstName: "Ella",
			lastName: "Walk-in",
			email: "  ",
			phone: "",
			notes: null,
			marketingConsent: false,
		});
		expect(body.email).toBeNull();
		expect(body.phone).toBeNull();
		expect(body.notes).toBeNull();
	});
});

describe("customerInputFrom", () => {
	it("carries every current field into the full-replace body, overriding only what changed", () => {
		const customer = toCustomer(dto);
		expect(customerInputFrom(customer, { notes: "Now prefers evenings." })).toEqual({
			firstName: "Olivia",
			lastName: "Client",
			email: "olivia@example.test",
			phone: "+49 30 7770001",
			notes: "Now prefers evenings.",
			marketingConsent: true,
		});
	});
});
