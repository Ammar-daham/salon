"use client";

import { useParams } from "next/navigation";
import { useCustomer } from "@/lib/resources/customers/customers.hooks";
import Card from "@/components/ui/Card";
import EmptyState from "@/components/ui/EmptyState";
import StatusBadge from "@/components/ui/StatusBadge";
import { Skeleton } from "@/components/ui/Skeleton";
import { DocsIcon } from "@/icons";

export default function CustomerNotesView() {
	const params = useParams<{ id: string }>();
	const id = Number(params.id);
	const { data: customer, isPending } = useCustomer(Number.isNaN(id) ? null : id);

	if (isPending || !customer) return <Skeleton className="h-64 rounded-card" />;

	return (
		<div className="flex flex-col gap-4 md:gap-6">
			{customer.tags.length > 0 && (
				<Card title="Tags" description="Shorthand the team uses to spot preferences quickly.">
					<div className="flex flex-wrap gap-2">
						{customer.tags.map((tag) => (
							<StatusBadge key={tag} tone="neutral">
								{tag}
							</StatusBadge>
						))}
					</div>
				</Card>
			)}

			<Card title="Notes">
				{customer.notes ? (
					<p className="text-sm leading-relaxed text-ink">{customer.notes}</p>
				) : (
					<EmptyState
						className="border-0 bg-transparent py-8"
						icon={<DocsIcon className="size-6" />}
						title="No notes yet"
						description="Notes capture preferences, allergies and anything worth remembering before the next visit."
					/>
				)}
			</Card>

			<Card title="Editing notes">
				<p className="text-sm text-ink-muted">
					Saving notes needs somewhere to put them. There is no notes column, table or endpoint
					on the backend, so this view is read-only until that exists — a text box that
					silently discarded what you typed would be worse than none.
				</p>
			</Card>
		</div>
	);
}
