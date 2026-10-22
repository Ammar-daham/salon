import type { Id } from "@/lib/api/types";

/** What happened. The only kind today: an owner registered a salon, for a super admin to review. */
export type NotificationType = "BUSINESS_REGISTERED";

/** Named so it doesn't shadow the browser's own Notification. */
export interface AppNotification {
	id: Id;
	type: NotificationType;
	/** The salon it's about, by its name as it is now. */
	business: { id: Id; name: string } | null;
	createdAt: string;
	/** Null until the reader marks it read. */
	readAt: string | null;
}
