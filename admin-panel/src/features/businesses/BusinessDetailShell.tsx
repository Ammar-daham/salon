"use client";

import React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { can } from "@/lib/auth/permissions";
import { resolveBusinessScope } from "@/lib/auth/scope";
import { useBusiness, useDeleteBusiness } from "@/lib/resources/businesses/businesses.hooks";
import {
	BUSINESS_STATUS_LABELS,
	BUSINESS_STATUS_TONE,
} from "@/lib/resources/businesses/businesses.types";
import { getErrorMessage } from "@/lib/api/errors";

import PageHeader from "@/components/ui/PageHeader";
import EmptyState from "@/components/ui/EmptyState";
import Tabs from "@/components/ui/Tabs";
import StatusBadge from "@/components/ui/StatusBadge";
import Button from "@/components/ui/button/Button";
import ConfirmDialog from "@/components/ui/modal/ConfirmDialog";
import { Skeleton } from "@/components/ui/Skeleton";
import { useToast } from "@/components/ui/toast/ToastProvider";
import { BoxIcon, ChevronLeftIcon, ErrorIcon, LockIcon, TrashBinIcon } from "@/icons";

export const BusinessContext = React.createContext<number | null>(null);

export function useBusinessId(): number {
	const id = React.useContext(BusinessContext);
	if (id == null) throw new Error("useBusinessId must be used inside BusinessDetailShell");
	return id;
}

export default function BusinessDetailShell({
	id,
	children,
}: {
	id: number;
	children: React.ReactNode;
}) {
	const { user } = useAuth();
	const router = useRouter();
	const { toast } = useToast();
	const scope = resolveBusinessScope(user);
	const [confirmingDelete, setConfirmingDelete] = React.useState(false);

	const { data: business, isPending, isError, error, refetch } = useBusiness(id);
	const remove = useDeleteBusiness();

	/**
	 * Ownership guard, enforced in one place for every tab.
	 *
	 * The backend does no ownership check on business PUT/DELETE, so this is
	 * currently the only thing stopping an admin from opening another salon.
	 * It is a UI boundary, not a security one — the real fix is a backend ticket.
	 */
	const outOfScope = scope.kind === "business" && scope.businessId !== id;

	if (outOfScope) {
		return (
			<EmptyState
				icon={<LockIcon className="size-6" />}
				title="That salon isn't yours"
				description="You can only view and manage the salon your account is linked to."
				action={
					<Button variant="outline" onClick={() => router.push("/my-business")}>
						Go to my salon
					</Button>
				}
			/>
		);
	}

	if (isError) {
		return (
			<EmptyState
				icon={<ErrorIcon className="size-6" />}
				title="Couldn't load this salon"
				description={getErrorMessage(error)}
				action={<Button variant="outline" onClick={() => refetch()}>Try again</Button>}
			/>
		);
	}

	const canDelete = can(user, "business:delete");
	const showBackLink = can(user, "business:list");

	const tabs = [
		{ label: "Overview", href: `/businesses/${id}`, exact: true },
		{ label: "Services", href: `/businesses/${id}/services` },
		{ label: "Team", href: `/businesses/${id}/team` },
		{ label: "Appointments", href: `/businesses/${id}/appointments` },
		{ label: "Settings", href: `/businesses/${id}/settings` },
	];

	async function handleDelete() {
		if (!business) return;
		try {
			await remove.mutateAsync(business.id);
			toast({
				tone: "success",
				title: "Salon deleted",
				description: `${business.name} and its related records were removed.`,
			});
			router.push("/businesses");
		} catch (err) {
			toast({ tone: "error", title: "Couldn't delete salon", description: getErrorMessage(err) });
		}
	}

	return (
		<BusinessContext.Provider value={id}>
			{showBackLink && (
				<Link
					href="/businesses"
					className="mb-4 inline-flex items-center gap-1 text-sm text-ink-muted transition-colors hover:text-ink"
				>
					<ChevronLeftIcon className="size-4" />
					All salons
				</Link>
			)}

			{isPending ? (
				<div className="mb-6 flex flex-col gap-3">
					<Skeleton className="h-9 w-64" />
					<Skeleton className="h-4 w-96" />
				</div>
			) : business ? (
				<PageHeader
					title={business.name}
					description={business.description ?? undefined}
					actions={
						canDelete && (
							<Button
								variant="outline"
								onClick={() => setConfirmingDelete(true)}
								startIcon={<TrashBinIcon className="size-4" />}
								className="text-error-600 hover:bg-error-50 dark:hover:bg-error-500/10"
							>
								Delete
							</Button>
						)
					}
				/>
			) : (
				<EmptyState
					icon={<BoxIcon className="size-6" />}
					title="Salon not found"
					description="It may have been deleted."
				/>
			)}

			{business && (
				<>
					<div className="mb-4 flex items-center gap-3">
						<StatusBadge tone={BUSINESS_STATUS_TONE[business.status]}>
							{BUSINESS_STATUS_LABELS[business.status]}
						</StatusBadge>
						<span className="text-sm text-ink-subtle">
							Added{" "}
							{new Date(business.createdAt).toLocaleDateString(undefined, {
								year: "numeric",
								month: "long",
								day: "numeric",
							})}
						</span>
					</div>

					<Tabs items={tabs} className="mb-6" />

					{children}
				</>
			)}

			<ConfirmDialog
				isOpen={confirmingDelete}
				onClose={() => setConfirmingDelete(false)}
				onConfirm={handleDelete}
				loading={remove.isPending}
				title={`Delete ${business?.name ?? "this salon"}?`}
				confirmLabel="Delete salon"
				description={
					business ? (
						<>
							This permanently removes the salon along with its{" "}
							<strong className="text-ink">{business.services.length} services</strong>,{" "}
							<strong className="text-ink">{business.addresses.length} addresses</strong> and{" "}
							<strong className="text-ink">{business.contacts.length} contacts</strong>. This
							can&apos;t be undone.
						</>
					) : null
				}
			/>
		</BusinessContext.Provider>
	);
}
