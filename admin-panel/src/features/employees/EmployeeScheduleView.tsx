"use client";

import { useParams } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { can } from "@/lib/auth/permissions";
import { getErrorMessage } from "@/lib/api/errors";
import { useEmployee } from "@/lib/resources/employees/employees.hooks";
import { useReplaceWorkingHours, useWorkingHours } from "@/lib/resources/hours/hours.hooks";
import type { WeeklyInterval } from "@/lib/resources/hours/hours.types";
import Card from "@/components/ui/Card";
import EmptyState from "@/components/ui/EmptyState";
import Button from "@/components/ui/button/Button";
import { Skeleton } from "@/components/ui/Skeleton";
import { useToast } from "@/components/ui/toast/ToastProvider";
import { ErrorIcon } from "@/icons";
import WeeklyHoursEditor, { WeeklyHoursSummary } from "@/features/hours/WeeklyHoursEditor";

/** When a staff member works: their weekly shifts, on the salon's clock. */
export default function EmployeeScheduleView() {
	const params = useParams<{ id: string }>();
	const id = Number(params.id);
	const { user } = useAuth();
	const { toast } = useToast();

	// The detail shell has already loaded the employee; this reads the same cache entry.
	const { data: employee } = useEmployee(Number.isNaN(id) ? null : id);
	const businessId = employee?.businessId ?? null;
	const { data: week, isPending, isError, error, refetch, dataUpdatedAt } = useWorkingHours(
		businessId,
		employee ? id : null,
	);
	const replace = useReplaceWorkingHours();
	const canEdit = can(user, "employee:edit");

	if (isError) {
		return (
			<EmptyState
				icon={<ErrorIcon className="size-6" />}
				title="Couldn't load working hours"
				description={getErrorMessage(error)}
				action={<Button variant="outline" onClick={() => refetch()}>Try again</Button>}
			/>
		);
	}

	if (isPending || businessId == null) {
		return <Skeleton className="h-96 max-w-3xl rounded-card" />;
	}

	async function handleSave(intervals: WeeklyInterval[]) {
		try {
			await replace.mutateAsync({ businessId: businessId!, staffId: id, intervals });
			toast({ tone: "success", title: "Working hours saved" });
		} catch (err) {
			toast({ tone: "error", title: "Couldn't save working hours", description: getErrorMessage(err) });
		}
	}

	return (
		<div className="flex max-w-3xl flex-col gap-4 md:gap-6">
			<Card
				title="Weekly hours"
				description="A day without shifts is a day off. Shifts may run outside opening hours, but only times when the salon is open too are offered for booking."
			>
				<p className="mb-4 text-sm text-ink-muted">
					Times are on the salon&apos;s clock, {week.timezone.replaceAll("_", " ")}.
				</p>

				{canEdit ? (
					<WeeklyHoursEditor
						key={dataUpdatedAt}
						initial={week.intervals}
						emptyLabel="Day off"
						addLabel="Add shift"
						saving={replace.isPending}
						onSave={handleSave}
					/>
				) : (
					<WeeklyHoursSummary intervals={week.intervals} emptyLabel="Day off" />
				)}
			</Card>
		</div>
	);
}
