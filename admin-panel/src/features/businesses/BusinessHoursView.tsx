"use client";

import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { can } from "@/lib/auth/permissions";
import { getErrorMessage } from "@/lib/api/errors";
import { useOpeningHours, useReplaceOpeningHours } from "@/lib/resources/hours/hours.hooks";
import type { WeeklyInterval } from "@/lib/resources/hours/hours.types";
import Card from "@/components/ui/Card";
import EmptyState from "@/components/ui/EmptyState";
import Button from "@/components/ui/button/Button";
import { Skeleton } from "@/components/ui/Skeleton";
import { useToast } from "@/components/ui/toast/ToastProvider";
import { ErrorIcon } from "@/icons";
import WeeklyHoursEditor, { WeeklyHoursSummary } from "@/features/hours/WeeklyHoursEditor";
import { useBusinessId } from "./BusinessDetailShell";

/**
 * The salon's weekly opening hours. Everyone who can see the salon sees them; its ADMIN or a
 * SUPER_ADMIN can change them. Availability only offers times inside them.
 */
export default function BusinessHoursView() {
	const businessId = useBusinessId();
	const { user } = useAuth();
	const { toast } = useToast();

	const { data: week, isPending, isError, error, refetch, dataUpdatedAt } = useOpeningHours(businessId);
	const replace = useReplaceOpeningHours();
	const canEdit = can(user, "business:edit");

	if (isError) {
		return (
			<EmptyState
				icon={<ErrorIcon className="size-6" />}
				title="Couldn't load opening hours"
				description={getErrorMessage(error)}
				action={<Button variant="outline" onClick={() => refetch()}>Try again</Button>}
			/>
		);
	}

	if (isPending) {
		return <Skeleton className="h-96 max-w-3xl rounded-card" />;
	}

	async function handleSave(intervals: WeeklyInterval[]) {
		try {
			await replace.mutateAsync({ businessId, intervals });
			toast({ tone: "success", title: "Opening hours saved" });
		} catch (err) {
			toast({ tone: "error", title: "Couldn't save opening hours", description: getErrorMessage(err) });
		}
	}

	return (
		<div className="max-w-3xl">
			<Card
				title="Opening hours"
				description="A day without hours is closed. Add a second interval for a lunch break."
			>
				<p className="mb-4 text-sm text-ink-muted">
					Times are on the salon&apos;s clock, {week.timezone.replaceAll("_", " ")}.
					{canEdit && (
						<>
							{" "}
							<Link
								href={`/businesses/${businessId}/settings`}
								className="font-medium text-primary-700 hover:underline dark:text-primary-300"
							>
								Change the time zone
							</Link>
						</>
					)}
				</p>

				{canEdit ? (
					// A save writes the stored week into the cache, and the new key starts the editor from it.
					<WeeklyHoursEditor
						key={dataUpdatedAt}
						initial={week.intervals}
						emptyLabel="Closed"
						addLabel="Add hours"
						saving={replace.isPending}
						onSave={handleSave}
					/>
				) : (
					<WeeklyHoursSummary intervals={week.intervals} emptyLabel="Closed" />
				)}
			</Card>
		</div>
	);
}
