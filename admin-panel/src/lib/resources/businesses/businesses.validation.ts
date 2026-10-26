import type { NewAddressInput } from "./businesses.types";

export type AddressErrors = Partial<Record<keyof NewAddressInput, string>>;

/** Whether any of the address was filled in: an untouched one isn't sent at all. */
export function hasAddress(address: NewAddressInput): boolean {
	return Object.values(address).some((value) => value.trim() !== "");
}

/**
 * The backend's rules for an address, once any of it is given: street (up to 255), city and country (up
 * to 100) are required, and the postal code is optional, up to 20.
 */
export function addressErrors(address: NewAddressInput): AddressErrors {
	if (!hasAddress(address)) return {};
	const errors: AddressErrors = {};
	if (!address.street.trim()) errors.street = "Street is required for an address.";
	else if (address.street.trim().length > 255) errors.street = "Use at most 255 characters.";
	if (!address.city.trim()) errors.city = "City is required for an address.";
	else if (address.city.trim().length > 100) errors.city = "Use at most 100 characters.";
	if (!address.country.trim()) errors.country = "Country is required for an address.";
	else if (address.country.trim().length > 100) errors.country = "Use at most 100 characters.";
	if (address.postalCode.trim().length > 20) errors.postalCode = "Use at most 20 characters.";
	return errors;
}
