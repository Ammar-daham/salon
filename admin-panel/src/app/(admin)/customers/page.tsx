import type { Metadata } from "next";
import PagePlaceholder from "@/components/ui/PagePlaceholder";

export const metadata: Metadata = {
	title: "Customers · Salon Admin",
};

export default function CustomersPage() {
	return (
		<PagePlaceholder
			title="Customers"
			description="Clients, their visit history and their notes."
			phase="Phase 3"
			detail="Customer records arrive with the mock data layer, alongside employees."
		/>
	);
}
