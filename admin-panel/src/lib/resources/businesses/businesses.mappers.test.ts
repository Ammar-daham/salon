import { describe, expect, it } from "vitest";
import { toBusiness, toBusinessRequest, toSalonService, type BusinessDto } from "./businesses.mappers";

const dto: BusinessDto = {
	id: 1,
	name: "Glow Beauty Studio",
	description: "Hair and nails",
	image: "https://example.test/glow.png",
	status: "APPROVED",
	created_at: "2026-09-01T10:00:00Z",
	updated_at: null,
	addresses: [
		{
			id: 1,
			street: "Main St 1",
			city: "Berlin",
			country: "DE",
			postal_code: "10115",
			latitude: "52.52",
			longitude: "13.40",
			user_id: null,
		},
	],
	contacts: [{ id: 1, type: "phone", value: "+49 30 1234567", created_at: "2026-09-01T10:00:00Z" }],
	services: [
		{
			id: 1,
			name: "Haircut",
			description: null,
			duration_minutes: 45,
			price: 35.5,
			is_active: true,
		},
	],
};

describe("toBusiness", () => {
	it("maps the business and its children to camelCase, dropping wire-only fields", () => {
		expect(toBusiness(dto)).toEqual({
			id: 1,
			name: "Glow Beauty Studio",
			description: "Hair and nails",
			image: "https://example.test/glow.png",
			status: "APPROVED",
			createdAt: "2026-09-01T10:00:00Z",
			updatedAt: null,
			addresses: [
				{
					id: 1,
					street: "Main St 1",
					city: "Berlin",
					country: "DE",
					postalCode: "10115",
					latitude: "52.52",
					longitude: "13.40",
				},
			],
			contacts: [{ id: 1, type: "phone", value: "+49 30 1234567" }],
			services: [
				{
					id: 1,
					name: "Haircut",
					description: null,
					durationMinutes: 45,
					price: 35.5,
					isActive: true,
				},
			],
		});
	});

	it("turns null child collections into empty arrays", () => {
		const business = toBusiness({ ...dto, addresses: null, contacts: null, services: null });
		expect(business.addresses).toEqual([]);
		expect(business.contacts).toEqual([]);
		expect(business.services).toEqual([]);
	});

	it("normalises missing optional fields to null", () => {
		const { updated_at: _u, description: _d, ...rest } = dto;
		const business = toBusiness({
			...rest,
			addresses: [{ id: 2, street: "Side St 2", city: "Berlin", country: "DE" }],
		} as unknown as BusinessDto);
		expect(business.description).toBeNull();
		expect(business.updatedAt).toBeNull();
		expect(business.addresses[0]).toMatchObject({ postalCode: null, latitude: null, longitude: null });
	});
});

describe("toSalonService", () => {
	it("maps a service on its own", () => {
		expect(
			toSalonService({ id: 3, name: "Fade", description: "Short", duration_minutes: 30, price: 20, is_active: false }),
		).toEqual({ id: 3, name: "Fade", description: "Short", durationMinutes: 30, price: 20, isActive: false });
	});
});

describe("toBusinessRequest", () => {
	const input = { name: "Glow", description: null, image: "https://example.test/glow.png" };

	it("omits status when not given, so the stored status is preserved", () => {
		expect(toBusinessRequest(input)).toEqual(input);
		expect(toBusinessRequest(input)).not.toHaveProperty("status");
	});

	it("sends status when given", () => {
		expect(toBusinessRequest({ ...input, status: "PENDING" })).toEqual({ ...input, status: "PENDING" });
	});
});
