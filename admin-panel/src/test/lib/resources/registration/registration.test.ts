import { describe, expect, it } from "vitest";
import { toRegistrationRequest } from "@/lib/resources/registration/registration.mappers";
import type { RegistrationInput } from "@/lib/resources/registration/registration.types";
import { conflictField, registrationErrors } from "@/lib/resources/registration/registration.validation";

const sara: RegistrationInput = {
	firstName: "Sara",
	lastName: "Owner",
	email: "sara@lumen.test",
	password: "Password123!",
	salonName: "Lumen Nail Bar",
	description: "Nails and lashes.",
	image: "https://img.test/lumen.png",
	currency: "EUR",
	timezone: "Europe/Berlin",
	street: "5 Torstr.",
	postalCode: "10119",
	city: "Berlin",
	country: "Germany",
	phone: "+49 30 5550100",
};

describe("registrationErrors", () => {
	it("passes a complete registration", () => {
		expect(registrationErrors(sara, "Password123!")).toEqual({});
	});

	it("asks for everything required, but not the optional fields", () => {
		const blank = Object.fromEntries(Object.keys(sara).map((k) => [k, " "])) as unknown as RegistrationInput;
		const errors = registrationErrors(blank, " ");
		expect(Object.keys(errors).sort()).toEqual(
			["city", "country", "email", "firstName", "image", "lastName", "password", "salonName", "street"].sort(),
		);
		expect(errors.salonName).toBe("Your salon's name is required.");
		expect(errors.password).toBe("Password is required.");
	});

	it("checks the email, the password rule and that the passwords match", () => {
		const errors = registrationErrors({ ...sara, email: "not-an-email", password: "short" }, "other");
		expect(errors.email).toBe("Enter a valid email address.");
		expect(errors.password).toBe("Use at least 8 characters.");
		expect(errors.confirmPassword).toBe("Passwords don't match.");
	});

	it("holds to the backend's lengths", () => {
		const errors = registrationErrors({ ...sara, salonName: "x".repeat(101), postalCode: "1".repeat(21) }, "Password123!");
		expect(errors.salonName).toBe("Use at most 100 characters.");
		expect(errors.postalCode).toBe("Use at most 20 characters.");
	});
});

describe("toRegistrationRequest", () => {
	it("sends the owner and the salon with its address and phone", () => {
		expect(toRegistrationRequest({ ...sara, salonName: "  Lumen Nail Bar " })).toEqual({
			owner: { first_name: "Sara", last_name: "Owner", email: "sara@lumen.test", password: "Password123!" },
			salon: {
				name: "Lumen Nail Bar",
				description: "Nails and lashes.",
				image: "https://img.test/lumen.png",
				currency: "EUR",
				timezone: "Europe/Berlin",
				addresses: [{ street: "5 Torstr.", city: "Berlin", country: "Germany", postal_code: "10119" }],
				contacts: [{ type: "phone", value: "+49 30 5550100" }],
			},
		});
	});

	it("leaves out what wasn't filled in", () => {
		const request = toRegistrationRequest({ ...sara, description: " ", postalCode: "", phone: "" });
		expect(request.salon.description).toBeNull();
		expect(request.salon.addresses[0]).not.toHaveProperty("postal_code");
		expect(request.salon.contacts).toEqual([]);
	});

	it("never sends a status: a registered salon always starts pending", () => {
		expect(request(sara)).not.toHaveProperty("status");
	});
});

function request(input: RegistrationInput) {
	return toRegistrationRequest(input).salon;
}

describe("conflictField", () => {
	it("puts each 409 by the field it's about", () => {
		expect(conflictField("Business with name Glow Beauty Studio already exists.")).toBe("salonName");
		expect(conflictField("Email already in use.")).toBe("email");
		expect(conflictField("Contact already exists.")).toBe("phone");
		expect(conflictField("Business could not be created due to a conflict.")).toBeNull();
	});
});
