import { describe, expect, it } from "vitest";
import { addressErrors, hasAddress } from "@/lib/resources/businesses/businesses.validation";

const empty = { street: "", postalCode: "", city: "", country: "" };

describe("addressErrors", () => {
	it("passes no address at all, since it's optional", () => {
		expect(hasAddress({ ...empty, city: "  " })).toBe(false);
		expect(addressErrors(empty)).toEqual({});
	});

	it("asks for street, city and country once any of it is given", () => {
		expect(addressErrors({ ...empty, postalCode: "10119" })).toEqual({
			street: "Street is required for an address.",
			city: "City is required for an address.",
			country: "Country is required for an address.",
		});
	});

	it("passes a full address, and holds to the backend's lengths", () => {
		const address = { street: "5 Torstr.", postalCode: "10119", city: "Berlin", country: "Germany" };
		expect(addressErrors(address)).toEqual({});
		expect(addressErrors({ ...address, postalCode: "1".repeat(21) }).postalCode).toBe("Use at most 20 characters.");
	});
});
