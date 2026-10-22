"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/query/queryKeys";
import type { Id } from "@/lib/api/types";
import {
	getUnreadNotificationCount,
	listNotifications,
	markAllNotificationsRead,
	markNotificationRead,
	type NotificationPageQuery,
} from "./notifications.api";

/** How often the unread count is asked for again while the panel is open. */
export const UNREAD_COUNT_REFRESH_MS = 60_000;

/** enabled is false until it's needed, e.g. while the bell's dropdown is closed. */
export function useNotificationPage(query: NotificationPageQuery, enabled = true) {
	return useQuery({
		queryKey: queryKeys.notifications.page(query),
		queryFn: ({ signal }) => listNotifications(query, { signal }),
		placeholderData: keepPreviousData,
		enabled,
	});
}

/**
 * The badge count. There's no push from the server, so it's asked for again every minute, and
 * whenever the window comes back into focus.
 */
export function useUnreadNotificationCount(enabled = true) {
	return useQuery({
		queryKey: queryKeys.notifications.unreadCount(),
		queryFn: ({ signal }) => getUnreadNotificationCount({ signal }),
		enabled,
		refetchInterval: UNREAD_COUNT_REFRESH_MS,
		refetchOnWindowFocus: true,
	});
}

export function useMarkNotificationRead() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: (id: Id) => markNotificationRead(id),
		onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.notifications.all }),
	});
}

export function useMarkAllNotificationsRead() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: () => markAllNotificationsRead(),
		onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.notifications.all }),
	});
}
