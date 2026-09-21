import type { Metadata } from "next";
import PagePlaceholder from "@/components/ui/PagePlaceholder";

export const metadata: Metadata = {
	title: "Services · Salon Admin",
};

export default function ServicesPage() {
	return (
		<PagePlaceholder
			title="Services"
			description="The treatments a salon offers, with duration and pricing."
			phase="Phase 2"
			detail="Service management is wired to live data and ships with the users work."
		/>
	);
}
