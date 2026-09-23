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
	latitude: string | null;
	longitude: string | null;
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
		createdAt: dto.created_at,
		updatedAt: dto.updated_at ?? null,
		addresses: (dto.addresses ?? []).map(toAddress),
		contacts: (dto.contacts ?? []).map(toContact),
		services: (dto.services ?? []).map(toSalonService),
	};
}

/**
 * Create/update body. Only these four fields are read by the backend.
 *
 * `status` is sent only when provided: the update SQL uses COALESCE, so omitting
 * it preserves the stored value instead of silently resetting the salon.
 * Children cannot be created through this endpoint — there is no POST for
 * addresses or contacts, and a nested address with a null id no-ops.
 */
export function toBusinessRequest(input: BusinessInput) {
	return {
		name: input.name,
		description: input.description,
		image: input.image,
		...(input.status ? { status: input.status } : {}),
	};
}
