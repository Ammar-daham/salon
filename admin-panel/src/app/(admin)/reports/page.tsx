import type { Metadata } from "next";
import PagePlaceholder from "@/components/ui/PagePlaceholder";

export const metadata: Metadata = {
	title: "Reports · Salon Admin",
};

export default function ReportsPage() {
	return (
		<PagePlaceholder
			title="Reports"
			description="Revenue, service mix and team performance."
			phase="Phase 6"
			detail="Analytics build on the appointment data, so they follow the scheduling work."
		/>
	);
}
