import type { Metadata } from "next";
import PagePlaceholder from "@/components/ui/PagePlaceholder";

export const metadata: Metadata = {
	title: "Billing · Salon Admin",
};

export default function SubscriptionsBillingPage() {
	return (
		<PagePlaceholder
			title="Billing"
			description="Your plan, invoices and payment details."
			phase="Phase 6"
			detail="Your salon's own billing view ships alongside reports and notifications."
		/>
	);
}
