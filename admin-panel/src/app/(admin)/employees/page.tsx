import type { Metadata } from "next";
import PagePlaceholder from "@/components/ui/PagePlaceholder";

export const metadata: Metadata = {
	title: "Employees · Salon Admin",
};

export default function EmployeesPage() {
	return (
		<PagePlaceholder
			title="Employees"
			description="Staff, their schedules and their availability."
			phase="Phase 3"
			detail="Creating staff already works through Add staff; the roster, schedules and profiles come with the mock data layer."
		/>
	);
}
