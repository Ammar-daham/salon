import type { Metadata } from "next";
import NotificationListView from "@/features/notifications/NotificationListView";

export const metadata: Metadata = {
	title: "Notifications · Salonat Pro",
};

export default function NotificationsPage() {
	return <NotificationListView />;
}
