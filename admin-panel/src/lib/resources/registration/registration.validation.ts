import { newPasswordErrors } from "@/lib/resources/auth/auth.validation";
import type { RegistrationInput } from "./registration.types";

export type RegistrationField = keyof RegistrationInput | "confirmPassword";
export type RegistrationErrors = Partial<Record<RegistrationField, string>>;

/** The same limits POST /auth/register checks, so most mistakes are caught before it's sent. */
const LIMITS: Partial<Record<keyof RegistrationInput, number>> = {
	firstName: 100,
	lastName: 100,
	salonName: 100,
	street: 255,
	city: 100,
	country: 100,
	postalCode: 20,
	phone: 255,
};

const REQUIRED: Partial<Record<keyof RegistrationInput, string>> = {
	firstName: "First name is required.",
	lastName: "Last name is required.",
	salonName: "Your salon's name is required.",
	image: "An image URL is required.",
	street: "Street is required.",
	city: "City is required.",
	country: "Country is required.",
};

/** What's wrong with a registration, by field. Empty when nothing is. */
export function registrationErrors(input: RegistrationInput, confirmPassword: string): RegistrationErrors {
	const errors: RegistrationErrors = {};
	for (const [field, message] of Object.entries(REQUIRED) as [keyof RegistrationInput, string][]) {
		if (!input[field].trim()) errors[field] = message;
	}
	for (const [field, max] of Object.entries(LIMITS) as [keyof RegistrationInput, number][]) {
		if (!errors[field] && input[field].trim().length > max) errors[field] = `Use at most ${max} characters.`;
	}

	const email = input.email.trim();
	if (!email) errors.email = "Email is required.";
	else if (!/^\S+@\S+\.\S+$/.test(email)) errors.email = "Enter a valid email address.";

	return { ...errors, ...newPasswordErrors(input.password, confirmPassword) };
}

/**
 * Which field a 409 from POST /auth/register is about, from its message, so it shows by that field;
 * null for one that isn't about a field.
 */
export function conflictField(message: string): RegistrationField | null {
	if (message.startsWith("Business with name")) return "salonName";
	if (message === "Email already in use.") return "email";
	if (message === "Contact already exists.") return "phone";
	return null;
}
