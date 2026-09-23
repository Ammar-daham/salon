import type { Metadata } from "next";
import EmptyState from "@/components/ui/EmptyState";
import { UserCircleIcon } from "@/icons";

export const metadata: Metadata = { title: "Team · Salon Admin" };

export default function BusinessTeamPage() {
	return (
		<EmptyState
			icon={<UserCircleIcon className="size-6" />}
			title="Team roster arrives in Phase 3"
			description="Staff records, schedules and availability need a backend that doesn't exist yet — the staff table has no working data access layer."
		/>
	);
}
