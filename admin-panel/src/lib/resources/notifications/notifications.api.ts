import { apiClient } from "@/lib/api/client";
import { endpoints } from "@/lib/api/endpoints";
import { pageParams, toPage, type PageDto } from "@/lib/api/paging";
import type { Id, ListParams, Page } from "@/lib/api/types";
import type { AppNotification } from "./notifications.types";
import { toNotification, type NotificationDto } from "./notifications.mappers";

/** Always newest first. unread leaves out the ones already read. */
export interface NotificationPageQuery {
	page?: number;
	size?: number;
	unread?: boolean;
}

/** Only ever the signed-in user's own: the server decides whose. */
export async function listNotifications(
	query: NotificationPageQuery,
	params?: ListParams,
): Promise<Page<AppNotification>> {
	const { data } = await apiClient.get<PageDto<NotificationDto>>(endpoints.notifications.root, {
		params: pageParams({ page: query.page, size: query.size }, { unread: query.unread ? "true" : undefined }),
		signal: params?.signal,
	});
	return toPage(data, toNotification);
}

export async function getUnreadNotificationCount(params?: ListParams): Promise<number> {
	const { data } = await apiClient.get<{ count: number }>(endpoints.notifications.unreadCount, {
		signal: params?.signal,
	});
	return data.count;
}

/** Answers with the notification as stored; one already read keeps when it first was. */
export async function markNotificationRead(id: Id): Promise<AppNotification> {
	const { data } = await apiClient.put<NotificationDto>(endpoints.notifications.read(id));
	return toNotification(data);
}

export async function markAllNotificationsRead(): Promise<void> {
	await apiClient.put(endpoints.notifications.readAll);
}
