import { apiClient } from "@/lib/api/client";
import { endpoints } from "@/lib/api/endpoints";
import type { Id, ListParams } from "@/lib/api/types";
import { toOpeningHours, toOpeningHoursRequest, type BusinessHoursDto } from "./hours.mappers";
import type { WeeklyHours, WeeklyInterval } from "./hours.types";

/** Anyone who can see the salon can read its hours; a pending salon is a 404 outside it. */
export async function getOpeningHours(businessId: Id, params?: ListParams): Promise<WeeklyHours> {
	const { data } = await apiClient.get<BusinessHoursDto>(endpoints.businesses.hours(businessId), {
		signal: params?.signal,
	});
	return toOpeningHours(data);
}

/** Replaces the whole week. The response is the week as stored, so no refetch is needed. */
export async function replaceOpeningHours(businessId: Id, intervals: WeeklyInterval[]): Promise<WeeklyHours> {
	const { data } = await apiClient.put<BusinessHoursDto>(
		endpoints.businesses.hours(businessId),
		toOpeningHoursRequest(intervals),
	);
	return toOpeningHours(data);
}
