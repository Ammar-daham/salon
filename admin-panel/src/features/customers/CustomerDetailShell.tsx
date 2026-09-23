"use client";

import Link from "next/link";
import { useCustomer } from "@/lib/resources/customers/customers.hooks";
import PageHeader from "@/components/ui/PageHeader";
import EmptyState from "@/components/ui/EmptyState";
import Tabs from "@/components/ui/Tabs";
import StatusBadge from "@/components/ui/StatusBadge";
import InitialsAvatar from "@/components/ui/InitialsAvatar";
import { Skeleton } from "@/components/ui/Skeleton";
import { ChevronLeftIcon, UserIcon } from "@/icons";

export default function CustomerDetailShell({
	id,
	children,
}: {
	id: number;
	children: React.ReactNode;
}) {
	const { data: customer, isPending, source } = useCustomer(id);

	if (isPending) {
		return (
			<div className="flex flex-col gap-4">
				<Skeleton className="h-6 w-32" />
				<Skeleton className="h-16 w-full max-w-md" />
				<Skeleton className="h-64 rounded-card" />
			</div>
		);
	}

	if (!customer) {
		return (
			<EmptyState
				icon={<UserIcon className="size-6" />}
				title="Client not found"
				description="This record may have been removed."
			/>
		);
	}

	const tabs = [
		{ label: "Profile", href: `/customers/${id}`, exact: true },
		{ label: "Appointments", href: `/customers/${id}/appointments` },
		{ label: "Notes", href: `/customers/${id}/notes` },
	];

	return (
		<>
			<Link
				href="/customers"
				className="mb-4 inline-flex items-center gap-1 text-sm text-ink-muted transition-colors hover:text-ink"
			>
				<ChevronLeftIcon className="size-4" />
				All customers
			</Link>

			<div className="mb-6 flex items-center gap-4">
				<InitialsAvatar
					firstName={customer.firstName}
					lastName={customer.lastName}
					size="lg"
					muted
				/>
				<PageHeader
					className="mb-0"
					title={`${customer.firstName} ${customer.lastName}`}
					description={customer.businessName}
					sampleData={source === "mock"}
				/>
			</div>

			{customer.tags.length > 0 && (
				<div className="mb-4 flex flex-wrap gap-2">
					{customer.tags.map((tag) => (
						<StatusBadge key={tag} tone="neutral">
							{tag}
						</StatusBadge>
					))}
				</div>
			)}

			<Tabs items={tabs} className="mb-6" />
			{children}
		</>
	);
}
