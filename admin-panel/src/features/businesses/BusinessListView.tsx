"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { can } from "@/lib/auth/permissions";
import { useServerTable, useStayOnAPage } from "@/lib/table/useServerTable";
import { useBusinessPage, useDeleteBusiness } from "@/lib/resources/businesses/businesses.hooks";
import {
	BUSINESS_STATUSES,
	BUSINESS_STATUS_LABELS,
	BUSINESS_STATUS_TONE,
	type Business,
	type BusinessStatus,
} from "@/lib/resources/businesses/businesses.types";
import { getErrorMessage } from "@/lib/api/errors";

import PageHeader from "@/components/ui/PageHeader";
import EmptyState from "@/components/ui/EmptyState";
import DataTable, { type Column } from "@/components/ui/DataTable";
import SearchInput from "@/components/ui/SearchInput";
import Pagination from "@/components/ui/Pagination";
import StatusBadge from "@/components/ui/StatusBadge";
import Button from "@/components/ui/button/Button";
import ConfirmDialog from "@/components/ui/modal/ConfirmDialog";
import { SelectInput } from "@/components/ui/form/Field";
import { useToast } from "@/components/ui/toast/ToastProvider";
import { BoxIcon, PlusIcon, TrashBinIcon, ErrorIcon } from "@/icons";

function cityOf(business: Business): string {
	return business.addresses[0]?.city ?? "—";
}

export default function BusinessListView() {
	const { user } = useAuth();
	const router = useRouter();
	const { toast } = useToast();

	const [statusFilter, setStatusFilter] = useState<BusinessStatus | "ALL">("ALL");
	const [pendingDelete, setPendingDelete] = useState<Business | null>(null);

	// Searched, filtered, sorted and paged by the server (BE-15). Sort keys are the API's.
	const table = useServerTable({ initialSort: { key: "name", direction: "asc" }, pageSize: 10 });
	const { data, isPending, isError, error, refetch } = useBusinessPage({
		...table.query,
		status: statusFilter === "ALL" ? undefined : statusFilter,
	});
	useStayOnAPage(table, data);
	const remove = useDeleteBusiness();

	const canCreate = can(user, "business:create");
	const canDelete = can(user, "business:delete");
	const isFiltered = table.isSearching || statusFilter !== "ALL";
	const total = data?.totalItems ?? 0;

	async function handleDelete() {
		if (!pendingDelete) return;
		const target = pendingDelete;
		try {
			await remove.mutateAsync(target.id);
			toast({
				tone: "success",
				title: "Salon deleted",
				description: `${target.name} and its related records were removed.`,
			});
			setPendingDelete(null);
		} catch (err) {
			toast({ tone: "error", title: "Couldn't delete salon", description: getErrorMessage(err) });
		}
	}

	const columns: Column<Business>[] = [
		{
			key: "name",
			header: "Salon",
			sortable: true,
			render: (b) => (
				<div className="flex items-center gap-3">
					{/* eslint-disable-next-line @next/next/no-img-element -- next/image has no
					    remotePatterns configured and business images are arbitrary client-supplied
					    URLs (data: URIs included), which next/image rejects outright. */}
					<img
						src={b.image}
						alt=""
						className="size-10 shrink-0 rounded-lg border border-border-default object-cover"
						onError={(e) => {
							e.currentTarget.style.visibility = "hidden";
						}}
					/>
					<div className="min-w-0">
						<Link
							href={`/businesses/${b.id}`}
							className="font-medium text-ink hover:text-primary-700 dark:hover:text-primary-300"
						>
							{b.name}
						</Link>
						{b.description && (
							<p className="truncate text-xs text-ink-subtle">{b.description}</p>
						)}
					</div>
				</div>
			),
		},
		{
			key: "status",
			header: "Status",
			sortable: true,
			render: (b) => (
				<StatusBadge tone={BUSINESS_STATUS_TONE[b.status]}>
					{BUSINESS_STATUS_LABELS[b.status]}
				</StatusBadge>
			),
		},
		{ key: "city", header: "City", render: (b) => cityOf(b) },
		{
			key: "services",
			header: "Services",
			render: (b) => <span className="tabular-nums">{b.services.length}</span>,
		},
		{
			key: "created_at",
			header: "Added",
			sortable: true,
			render: (b) => (
				<span className="text-ink-muted">
					{new Date(b.createdAt).toLocaleDateString(undefined, {
						year: "numeric",
						month: "short",
						day: "numeric",
					})}
				</span>
			),
		},
		{
			key: "actions",
			header: "",
			align: "right",
			render: (b) => (
				<div className="flex justify-end gap-2">
					<Button size="sm" variant="outline" onClick={() => router.push(`/businesses/${b.id}`)}>
						View
					</Button>
					{canDelete && (
						<Button
							size="sm"
							variant="ghost"
							aria-label={`Delete ${b.name}`}
							onClick={() => setPendingDelete(b)}
							startIcon={<TrashBinIcon className="size-4" />}
							className="text-error-600 hover:bg-error-50 hover:text-error-700 dark:hover:bg-error-500/10"
						>
							Delete
						</Button>
					)}
				</div>
			),
		},
	];

	return (
		<>
			<PageHeader
				title="Businesses"
				description="Every salon on the platform."
				actions={
					canCreate && (
						<Button startIcon={<PlusIcon className="size-4" />} onClick={() => router.push("/businesses/new")}>
							New salon
						</Button>
					)
				}
			/>

			{isError ? (
				<EmptyState
					icon={<ErrorIcon className="size-6" />}
					title="Couldn't load salons"
					description={getErrorMessage(error)}
					action={<Button variant="outline" onClick={() => refetch()}>Try again</Button>}
				/>
			) : (
				<>
					<div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
						<SearchInput
							value={table.search}
							onChange={table.setSearch}
							placeholder="Search by name, description or city…"
							aria-label="Search salons"
							className="sm:max-w-sm"
						/>
						<SelectInput
							aria-label="Filter by status"
							value={statusFilter}
							onChange={(e) => {
								setStatusFilter(e.target.value as BusinessStatus | "ALL");
								table.firstPage();
							}}
							className="sm:w-48"
						>
							<option value="ALL">All statuses</option>
							{BUSINESS_STATUSES.map((s) => (
								<option key={s} value={s}>
									{BUSINESS_STATUS_LABELS[s]}
								</option>
							))}
						</SelectInput>
						{!isPending && (
							<p className="text-sm text-ink-subtle sm:ml-auto">
								{total} {total === 1 ? "salon" : "salons"}
							</p>
						)}
					</div>

					<DataTable
						columns={columns}
						rows={data?.items ?? []}
						rowKey={(b) => b.id}
						loading={isPending}
						sort={table.sort}
						onSort={table.toggleSort}
						empty={
							isFiltered ? (
								<EmptyState
									icon={<BoxIcon className="size-6" />}
									title="No salons match your filters"
									description="Try a different search term, or clear the status filter."
									action={
										<Button
											variant="outline"
											onClick={() => {
												table.setSearch("");
												setStatusFilter("ALL");
											}}
										>
											Clear filters
										</Button>
									}
								/>
							) : (
								<EmptyState
									icon={<BoxIcon className="size-6" />}
									title="No salons yet"
									description="Add the first salon to start managing its team, services and bookings."
									action={
										canCreate && (
											<Button
												startIcon={<PlusIcon className="size-4" />}
												onClick={() => router.push("/businesses/new")}
											>
												New salon
											</Button>
										)
									}
								/>
							)
						}
					/>

					{!isPending && data && total > 0 && (
						<div className="mt-px rounded-b-card border border-t-0 border-border-default bg-surface-raised">
							<Pagination
								page={table.page}
								pageCount={data.totalPages}
								total={total}
								pageSize={data.size}
								onPageChange={table.setPage}
							/>
						</div>
					)}
				</>
			)}

			<ConfirmDialog
				isOpen={pendingDelete !== null}
				onClose={() => setPendingDelete(null)}
				onConfirm={handleDelete}
				loading={remove.isPending}
				title={`Delete ${pendingDelete?.name ?? "this salon"}?`}
				confirmLabel="Delete salon"
				description={
					pendingDelete ? (
						<>
							This permanently removes the salon along with its{" "}
							<strong className="text-ink">
								{pendingDelete.services.length}{" "}
								{pendingDelete.services.length === 1 ? "service" : "services"}
							</strong>
							,{" "}
							<strong className="text-ink">
								{pendingDelete.addresses.length}{" "}
								{pendingDelete.addresses.length === 1 ? "address" : "addresses"}
							</strong>{" "}
							and{" "}
							<strong className="text-ink">
								{pendingDelete.contacts.length}{" "}
								{pendingDelete.contacts.length === 1 ? "contact" : "contacts"}
							</strong>
							. This can&apos;t be undone.
						</>
					) : null
				}
			/>
		</>
	);
}
