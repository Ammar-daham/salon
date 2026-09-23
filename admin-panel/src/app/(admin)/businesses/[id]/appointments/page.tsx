import type { Metadata } from "next";
import EmptyState from "@/components/ui/EmptyState";
import { TaskIcon } from "@/icons";

export const metadata: Metadata = { title: "Appointments · Salon Admin" };

export default function BusinessAppointmentsPage() {
	return (
		<EmptyState
			icon={<TaskIcon className="size-6" />}
			title="Bookings arrive in Phase 4"
			description="Appointments have no table, no endpoint and no status enum on the backend yet. This tab fills in once that domain is built."
		/>
	);
}
