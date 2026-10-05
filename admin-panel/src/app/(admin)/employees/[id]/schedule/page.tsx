import type { Metadata } from "next";
import EmptyState from "@/components/ui/EmptyState";
import { CalenderIcon } from "@/icons";

export const metadata: Metadata = { title: "Schedule · Salon Admin" };

export default function EmployeeSchedulePage() {
	return (
		<EmptyState
			icon={<CalenderIcon className="size-6" />}
			title="Working hours arrive in Phase 2"
			description="A weekly schedule needs a business-hours/staff-schedules table that doesn't exist on the backend yet - there is no model, no endpoint and no day-of-week concept. This tab fills in once that domain is built."
		/>
	);
}
