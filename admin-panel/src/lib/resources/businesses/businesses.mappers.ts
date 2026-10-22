import type {
	Address,
	Business,
	BusinessInput,
	BusinessStatus,
	Contact,
	SalonService,
} from "./businesses.types";

interface AddressDto {
	id: number;
	street: string;
	city: string;
	country: string;
	postal_code: string | null;
	latitude: number | null;
	longitude: number | null;
	created_at?: string;
	updated_at?: string | null;
	user_id?: number | null;
}

interface ContactDto {
	id: number;
	type: string;
	value: string;
	created_at?: string;
	updated_at?: string | null;
}

interface ServiceDto {
	id: number;
	name: string;
	description: string | null;
	duration_minutes: number;
	price: number;
	is_active: boolean;
	created_at?: string;
	updated_at?: string | null;
}

export interface BusinessDto {
	id: number;
	name: string;
	description: string | null;
	image: string;
	status: BusinessStatus;
	currency: string;
	timezone: string;
	created_at: string;
	updated_at: string | null;
	addresses: AddressDto[] | null;
	contacts: ContactDto[] | null;
	services: ServiceDto[] | null;
}

const toAddress = (dto: AddressDto): Address => ({
	id: dto.id,
	street: dto.street,
	city: dto.city,
	country: dto.country,
	postalCode: dto.postal_code ?? null,
	latitude: dto.latitude ?? null,
	longitude: dto.longitude ?? null,
});

const toContact = (dto: ContactDto): Contact => ({
	id: dto.id,
	type: dto.type,
	value: dto.value,
});

export const toSalonService = (dto: ServiceDto): SalonService => ({
	id: dto.id,
	name: dto.name,
	description: dto.description ?? null,
	durationMinutes: dto.duration_minutes,
	price: dto.price,
	isActive: dto.is_active,
});

export function toBusiness(dto: BusinessDto): Business {
	return {
		id: dto.id,
		name: dto.name,
		description: dto.description ?? null,
		image: dto.image,
		status: dto.status,
		currency: dto.currency,
		timezone: dto.timezone,
		createdAt: dto.created_at,
		updatedAt: dto.updated_at ?? null,
		addresses: (dto.addresses ?? []).map(toAddress),
		contacts: (dto.contacts ?? []).map(toContact),
		services: (dto.services ?? []).map(toSalonService),
	};
}

/**
 * Update body, and the start of the create body. The update replaces every one of these fields, so
 * each is always sent.
 *
 * `status` isn't: neither endpoint reads it. A new salon is PENDING, and only
 * PUT /businesses/{id}/status moves it. Nor are a salon's addresses and contacts, which the update
 * leaves alone: they're created with the salon (toCreateBusinessRequest), or one at a time afterwards.
 */
export function toBusinessRequest(input: BusinessInput) {
	return {
		name: input.name,
		description: input.description,
		image: input.image,
		currency: input.currency,
		timezone: input.timezone,
	};
}

/** Create body: the salon, with its address and phone if it has them. */
export function toCreateBusinessRequest(input: BusinessInput) {
	const address = input.address;
	const postalCode = address?.postalCode.trim();
	const phone = input.phone?.trim();
	return {
		...toBusinessRequest(input),
		addresses: address
			? [
					{
						street: address.street.trim(),
						city: address.city.trim(),
						country: address.country.trim(),
						...(postalCode ? { postal_code: postalCode } : {}),
					},
				]
			: [],
		contacts: phone ? [{ type: "phone", value: phone }] : [],
	};
}
