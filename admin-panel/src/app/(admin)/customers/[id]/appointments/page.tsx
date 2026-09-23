import type { Metadata } from "next";
import EmptyState from "@/components/ui/EmptyState";
import { TaskIcon } from "@/icons";

export const metadata: Metadata = { title: "Visit history · Salon Admin" };

export default function CustomerAppointmentsPage() {
	return (
		<EmptyState
			icon={<TaskIcon className="size-6" />}
			title="Visit history arrives in Phase 4"
			description="Past and upcoming bookings need the appointments domain, which has no table or endpoint on the backend yet."
		/>
	);
}
