import type { Metadata } from "next";
import PagePlaceholder from "@/components/ui/PagePlaceholder";

export const metadata: Metadata = {
	title: "Subscriptions · Salon Admin",
};

export default function SubscriptionsPage() {
	return (
		<PagePlaceholder
			title="Subscriptions"
			description="Plans, subscribers and platform billing."
			phase="Phase 6"
			detail="Plan management ships alongside reports and notifications."
		/>
	);
}
