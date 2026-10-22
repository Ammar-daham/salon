import { describe, expect, it } from "vitest";
import {
	type BusinessDto,
	toBusiness,
	toBusinessRequest,
	toCreateBusinessRequest,
	toSalonService,
} from "@/lib/resources/businesses/businesses.mappers";

const dto: BusinessDto = {
	id: 1,
	name: "Glow Beauty Studio",
	description: "Hair and nails",
	image: "https://example.test/glow.png",
	status: "APPROVED",
	currency: "EUR",
	timezone: "Europe/Berlin",
	created_at: "2026-09-01T10:00:00Z",
	updated_at: null,
	addresses: [
		{
			id: 1,
			street: "Main St 1",
			city: "Berlin",
			country: "DE",
			postal_code: "10115",
			latitude: 52.52,
			longitude: 13.4,
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
			currency: "EUR",
			timezone: "Europe/Berlin",
			createdAt: "2026-09-01T10:00:00Z",
			updatedAt: null,
			addresses: [
				{
					id: 1,
					street: "Main St 1",
					city: "Berlin",
					country: "DE",
					postalCode: "10115",
					latitude: 52.52,
					longitude: 13.4,
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
	const input = {
		name: "Glow",
		description: null,
		image: "https://example.test/glow.png",
		currency: "SEK",
		timezone: "Europe/London",
	};

	it("sends every field, as the update replaces them all", () => {
		expect(toBusinessRequest(input)).toEqual(input);
	});

	it("never sends a status, which only its own endpoint changes", () => {
		expect(toBusinessRequest({ ...input, status: "APPROVED" })).toEqual(input);
	});

	it("leaves a salon's addresses and contacts out of the update", () => {
		const update = toBusinessRequest({ ...input, address: { street: "x", postalCode: "", city: "y", country: "z" }, phone: "1" });
		expect(update).toEqual(input);
	});
});

describe("toCreateBusinessRequest", () => {
	const input = {
		name: "Lumen Nail Bar",
		description: null,
		image: "https://example.test/lumen.png",
		currency: "EUR",
		timezone: "Europe/Berlin",
	};

	it("creates the salon with its address and phone", () => {
		expect(
			toCreateBusinessRequest({
				...input,
				address: { street: " 5 Torstr. ", postalCode: "10119", city: "Berlin", country: "Germany" },
				phone: " +49 30 5550100 ",
			}),
		).toEqual({
			...input,
			addresses: [{ street: "5 Torstr.", city: "Berlin", country: "Germany", postal_code: "10119" }],
			contacts: [{ type: "phone", value: "+49 30 5550100" }],
		});
	});

	it("sends no address or phone when there's none", () => {
		expect(toCreateBusinessRequest({ ...input, address: null, phone: "" })).toEqual({
			...input,
			addresses: [],
			contacts: [],
		});
		const noPostalCode = toCreateBusinessRequest({
			...input,
			address: { street: "5 Torstr.", postalCode: " ", city: "Berlin", country: "Germany" },
		});
		expect(noPostalCode.addresses[0]).not.toHaveProperty("postal_code");
	});

	it("never sends a status", () => {
		expect(toCreateBusinessRequest({ ...input, status: "APPROVED" })).not.toHaveProperty("status");
	});
});
