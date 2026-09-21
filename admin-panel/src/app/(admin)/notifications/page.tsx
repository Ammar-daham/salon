import type { Metadata } from "next";
import PagePlaceholder from "@/components/ui/PagePlaceholder";

export const metadata: Metadata = {
	title: "Notifications · Salon Admin",
};

export default function NotificationsPage() {
	return (
		<PagePlaceholder
			title="Notifications"
			description="System alerts and activity across your salon."
			phase="Phase 6"
			detail="The notification feed and the header bell are wired up together."
		/>
	);
}
