import { apiClient } from "@/lib/api/client";
import { endpoints } from "@/lib/api/endpoints";
import type { Id, ListParams } from "@/lib/api/types";
import { businessesRepository } from "@/lib/resources/businesses/businesses.api";
import { toSalonService } from "@/lib/resources/businesses/businesses.mappers";
import type { SalonService, SalonServiceInput } from "@/lib/resources/businesses/businesses.types";

export const source = "live" as const;

/**
 * Services are snake_case on the wire in BOTH directions — verified against the
 * running API, where a camelCase body is rejected outright with
 * 400 INVALID_JSON. This is the only place that knows it.
 */
interface ServiceRequestDto {
	name: string;
	description: string | null;
	duration_minutes: number;
	price: number;
	is_active: boolean;
}

function toRequest(input: SalonServiceInput): ServiceRequestDto {
	return {
		name: input.name,
		description: input.description,
		duration_minutes: input.durationMinutes,
		price: input.price,
		is_active: input.isActive,
	};
}

/**
 * There is no list endpoint for services. The list is a projection of the
 * business response, so it reads through the same cache entry rather than
 * issuing a request of its own — which is also why every mutation below
 * invalidates the *business*, not a phantom services list.
 */
export async function listServices(
	businessId: Id,
	params?: ListParams,
): Promise<SalonService[]> {
	const business = await businessesRepository.get(businessId, params);
	return business.services;
}

export async function getService(
	businessId: Id,
	serviceId: Id,
	params?: ListParams,
): Promise<SalonService> {
	const { data } = await apiClient.get(endpoints.businesses.serviceById(businessId, serviceId), {
		signal: params?.signal,
	});
	return toSalonService(data);
}

export async function createService(
	businessId: Id,
	input: SalonServiceInput,
): Promise<SalonService> {
	const { data } = await apiClient.post(
		endpoints.businesses.services(businessId),
		toRequest(input),
	);
	return toSalonService(data);
}

export async function updateService(
	businessId: Id,
	serviceId: Id,
	input: SalonServiceInput,
): Promise<void> {
	await apiClient.put(
		endpoints.businesses.serviceById(businessId, serviceId),
		toRequest(input),
	);
}

/**
 * Unlike businesses and users, this DELETE takes no body — business_service is
 * ON DELETE CASCADE, so removing the service cleans up the join row itself.
 */
export async function removeService(businessId: Id, serviceId: Id): Promise<void> {
	await apiClient.delete(endpoints.businesses.serviceById(businessId, serviceId));
}

/**
 * services.name has no unique constraint and the backend's DuplicateKeyException
 * catch is unreachable dead code — posting the same name twice silently creates
 * two real rows. Guarding client-side is the only protection there is.
 */
export function isDuplicateName(
	existing: SalonService[],
	name: string,
	ignoreId?: Id,
): boolean {
	const needle = name.trim().toLowerCase();
	return existing.some((s) => s.id !== ignoreId && s.name.trim().toLowerCase() === needle);
}
