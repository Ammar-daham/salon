import { apiClient } from "@/lib/api/client";
import { endpoints } from "@/lib/api/endpoints";
import type { Id, ListParams } from "@/lib/api/types";
import {
	toOpeningHours,
	toOpeningHoursRequest,
	toWorkingHours,
	toWorkingHoursRequest,
	type BusinessHoursDto,
	type StaffScheduleDto,
} from "./hours.mappers";
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

/** The salon's staff (and a SUPER_ADMIN) can read a staff member's week; others get a 403. */
export async function getWorkingHours(businessId: Id, staffId: Id, params?: ListParams): Promise<WeeklyHours> {
	const { data } = await apiClient.get<StaffScheduleDto>(endpoints.businesses.staffSchedule(businessId, staffId), {
		signal: params?.signal,
	});
	return toWorkingHours(data);
}

export async function replaceWorkingHours(
	businessId: Id,
	staffId: Id,
	intervals: WeeklyInterval[],
): Promise<WeeklyHours> {
	const { data } = await apiClient.put<StaffScheduleDto>(
		endpoints.businesses.staffSchedule(businessId, staffId),
		toWorkingHoursRequest(intervals),
	);
	return toWorkingHours(data);
}
