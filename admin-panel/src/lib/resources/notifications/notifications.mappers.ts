import type { AppNotification, NotificationType } from "./notifications.types";

export interface NotificationDto {
	id: number;
	type: NotificationType;
	business: { id: number; name: string } | null;
	created_at: string;
	read_at: string | null;
}

export function toNotification(dto: NotificationDto): AppNotification {
	return {
		id: dto.id,
		type: dto.type,
		business: dto.business ? { id: dto.business.id, name: dto.business.name } : null,
		createdAt: dto.created_at,
		readAt: dto.read_at ?? null,
	};
}

/** What a notification says, and where it leads. */
export interface NotificationText {
	title: string;
	body: string;
	href: string;
}

export function notificationText(notification: AppNotification): NotificationText {
	switch (notification.type) {
		case "BUSINESS_REGISTERED":
			return {
				title: "New salon registration",
				body: notification.business
					? `${notification.business.name} is waiting for your review.`
					: "A salon is waiting for your review.",
				href: notification.business
					? `/businesses/${notification.business.id}`
					: "/businesses?status=PENDING",
			};
		default:
			// A type this panel doesn't know yet, from a newer backend.
			return { title: "Notification", body: "", href: "/notifications" };
	}
}
