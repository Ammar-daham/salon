"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/query/queryKeys";
import type { Id } from "@/lib/api/types";
import { getOpeningHours, getWorkingHours, replaceOpeningHours, replaceWorkingHours } from "./hours.api";
import type { WeeklyInterval } from "./hours.types";

export function useOpeningHours(businessId: Id | null) {
	return useQuery({
		queryKey: queryKeys.hours.business(businessId ?? -1),
		queryFn: ({ signal }) => getOpeningHours(businessId as Id, { signal }),
		enabled: businessId != null,
	});
}

/** The PUT answers with the stored week, which goes straight into the cache. */
export function useReplaceOpeningHours() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: ({ businessId, intervals }: { businessId: Id; intervals: WeeklyInterval[] }) =>
			replaceOpeningHours(businessId, intervals),
		onSuccess: (week, { businessId }) => qc.setQueryData(queryKeys.hours.business(businessId), week),
	});
}

/** businessId comes from the employee record, so the query waits until that has loaded. */
export function useWorkingHours(businessId: Id | null, staffId: Id | null) {
	return useQuery({
		queryKey: queryKeys.hours.staff(staffId ?? -1),
		queryFn: ({ signal }) => getWorkingHours(businessId as Id, staffId as Id, { signal }),
		enabled: businessId != null && staffId != null,
	});
}

export function useReplaceWorkingHours() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: ({ businessId, staffId, intervals }: { businessId: Id; staffId: Id; intervals: WeeklyInterval[] }) =>
			replaceWorkingHours(businessId, staffId, intervals),
		onSuccess: (week, { staffId }) => qc.setQueryData(queryKeys.hours.staff(staffId), week),
	});
}
