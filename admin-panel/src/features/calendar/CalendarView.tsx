"use client";

import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { resolveBusinessScope } from "@/lib/auth/scope";
import type { Id } from "@/lib/api/types";
import { useBusinesses } from "@/lib/resources/businesses/businesses.hooks";
import PageHeader from "@/components/ui/PageHeader";
import EmptyState from "@/components/ui/EmptyState";
import { SelectInput } from "@/components/ui/form/Field";
import { CalenderIcon } from "@/icons";
import SalonCalendar from "./SalonCalendar";

/**
 * One salon's week or day, on its own clock. A SUPER_ADMIN picks the salon, which the URL keeps
 * as ?business=; anyone else sees their own. ?staff= starts on one staff member.
 */
export default function CalendarView({ businessParam, staffParam }: { businessParam: Id | null; staffParam: Id | null }) {
	const router = useRouter();
	const { user } = useAuth();
	const scope = resolveBusinessScope(user);
	const { data: businesses, isPending: businessesPending } = useBusinesses(scope.kind === "platform");

	if (scope.kind === "unresolved") {
		return (
			<>
				<PageHeader title="Calendar" />
				<EmptyState
					icon={<CalenderIcon className="size-6" />}
					title="Your account isn't linked to a salon yet"
					description="Ask a platform administrator to attach your account to a business to see its calendar."
				/>
			</>
		);
	}

	const businessId = scope.kind === "business" ? scope.businessId : businessParam;

	return (
		<>
			<PageHeader
				title="Calendar"
				description="Appointments by day or week, for the whole team or one staff member."
				actions={
					scope.kind === "platform" && (
						<SelectInput
							aria-label="Salon"
							value={businessId ?? ""}
							onChange={(e) => router.replace(e.target.value ? `/calendar?business=${e.target.value}` : "/calendar")}
							disabled={businessesPending}
							className="sm:w-64"
						>
							<option value="">{businessesPending ? "Loading salons…" : "Select a salon…"}</option>
							{(businesses ?? []).map((b) => (
								<option key={b.id} value={b.id}>
									{b.name}
								</option>
							))}
						</SelectInput>
					)
				}
			/>
			{businessId == null ? (
				<EmptyState
					icon={<CalenderIcon className="size-6" />}
					title="Choose a salon"
					description="Each salon's calendar runs on its own clock, so it shows one at a time."
				/>
			) : (
				// Starts afresh, on that salon's today, when the salon changes.
				<SalonCalendar key={businessId} businessId={businessId} initialStaffId={staffParam} />
			)}
		</>
	);
}
