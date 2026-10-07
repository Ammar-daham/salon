"use client";

import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { can } from "@/lib/auth/permissions";
import { resolveBusinessScope } from "@/lib/auth/scope";
import PageHeader from "@/components/ui/PageHeader";
import EmptyState from "@/components/ui/EmptyState";
import Button from "@/components/ui/button/Button";
import { PlusIcon, TaskIcon } from "@/icons";
import AppointmentList from "./AppointmentList";
import { bookingHref } from "./appointmentLinks";

/** Every appointment the caller can see: their own salon's, or every salon's for a SUPER_ADMIN. */
export default function AppointmentListView() {
	const { user } = useAuth();
	const router = useRouter();
	const scope = resolveBusinessScope(user);

	if (scope.kind === "unresolved") {
		return (
			<>
				<PageHeader title="Appointments" />
				<EmptyState
					icon={<TaskIcon className="size-6" />}
					title="Your account isn't linked to a salon yet"
					description="Ask a platform administrator to attach your account to a business. Its appointments will appear here once linked."
				/>
			</>
		);
	}

	return (
		<>
			<PageHeader
				title="Appointments"
				description={
					scope.kind === "platform"
						? "Bookings across every salon on the platform, on each salon's own clock."
						: "Your salon's bookings. Filter by date, status or staff member."
				}
				actions={
					can(user, "appointment:create") && (
						<Button startIcon={<PlusIcon className="size-4" />} onClick={() => router.push(bookingHref())}>
							Book appointment
						</Button>
					)
				}
			/>
			<AppointmentList businessId={scope.kind === "business" ? scope.businessId : null} />
		</>
	);
}
