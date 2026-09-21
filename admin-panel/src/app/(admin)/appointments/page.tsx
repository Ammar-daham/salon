import type { Metadata } from "next";
import PagePlaceholder from "@/components/ui/PagePlaceholder";

export const metadata: Metadata = {
	title: "Appointments · Salon Admin",
};

export default function AppointmentsPage() {
	return (
		<PagePlaceholder
			title="Appointments"
			description="Every booking, filterable by employee, service and status."
			phase="Phase 4"
			detail="Bookings, the status workflow and conflict checking are built together with the calendar."
		/>
	);
}
