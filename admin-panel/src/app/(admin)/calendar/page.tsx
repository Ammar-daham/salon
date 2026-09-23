import type { Metadata } from "next";
import PagePlaceholder from "@/components/ui/PagePlaceholder";

export const metadata: Metadata = {
	title: "Calendar · Salon Admin",
};

export default function CalendarPage() {
	return (
		<PagePlaceholder
			title="Calendar"
			description="Day, week and month views across your team."
			phase="Phase 4"
			detail="The scheduling grid, drag-to-book and availability checks arrive together."
		/>
	);
}
