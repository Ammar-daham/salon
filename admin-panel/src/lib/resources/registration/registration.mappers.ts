import type { RegistrationInput } from "./registration.types";

/** The body of POST /auth/register: the owner, and the salon with its address and phone. */
export function toRegistrationRequest(input: RegistrationInput) {
	const description = input.description.trim();
	const postalCode = input.postalCode.trim();
	const phone = input.phone.trim();
	return {
		owner: {
			first_name: input.firstName.trim(),
			last_name: input.lastName.trim(),
			email: input.email.trim(),
			password: input.password,
		},
		salon: {
			name: input.salonName.trim(),
			description: description || null,
			image: input.image.trim(),
			currency: input.currency,
			timezone: input.timezone,
			addresses: [
				{
					street: input.street.trim(),
					city: input.city.trim(),
					country: input.country.trim(),
					...(postalCode ? { postal_code: postalCode } : {}),
				},
			],
			contacts: phone ? [{ type: "phone", value: phone }] : [],
		},
	};
}
