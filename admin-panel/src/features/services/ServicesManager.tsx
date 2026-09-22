"use client";

import React, { useMemo, useState } from "react";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { can } from "@/lib/auth/permissions";
import type { Id } from "@/lib/api/types";
import { useDataTable } from "@/lib/table/useDataTable";
import { useBusinesses } from "@/lib/resources/businesses/businesses.hooks";
import {
	useCreateService,
	useDeleteService,
	useUpdateService,
} from "@/lib/resources/services/services.hooks";
import type {
	SalonService,
	SalonServiceInput,
} from "@/lib/resources/businesses/businesses.types";
import { getErrorMessage } from "@/lib/api/errors";

import DataTable, { type Column } from "@/components/ui/DataTable";
import SearchInput from "@/components/ui/SearchInput";
import Pagination from "@/components/ui/Pagination";
import EmptyState from "@/components/ui/EmptyState";
import StatusBadge from "@/components/ui/StatusBadge";
import Button from "@/components/ui/button/Button";
import ConfirmDialog from "@/components/ui/modal/ConfirmDialog";
import { SelectInput } from "@/components/ui/form/Field";
import { useToast } from "@/components/ui/toast/ToastProvider";
import { ErrorIcon, ListIcon, PencilIcon, PlusIcon, TrashBinIcon } from "@/icons";
import ServiceFormModal from "./ServiceFormModal";

export interface ServiceRow extends SalonService {
	businessId: Id;
	businessName: string;
}

interface ServicesManagerProps {
	/** Fixed salon, for the scoped views. Null means every salon the viewer can see. */
	businessId: Id | null;
	/** Rendered above the table by the page; the manager owns everything below. */
	renderToolbarExtras?: React.ReactNode;
}

function money(value: number) {
	return new Intl.NumberFormat(undefined, { style: "currency", currency: "EUR" }).format(value);
}

export default function ServicesManager({ businessId }: ServicesManagerProps) {
	const { user } = useAuth();
	const { toast } = useToast();

	// One GET /business feeds both the scoped and the cross-salon view — the
	// services list has no endpoint of its own, so it is always a projection.
	const { data: businesses, isPending, isError, error, refetch } = useBusinesses();

	const create = useCreateService();
	const update = useUpdateService();
	const remove = useDeleteService();

	const [businessFilter, setBusinessFilter] = useState<string>("ALL");
	const [activeFilter, setActiveFilter] = useState<"ALL" | "ACTIVE" | "INACTIVE">("ALL");
	const [formOpen, setFormOpen] = useState(false);
	const [editing, setEditing] = useState<ServiceRow | null>(null);
	const [pendingDelete, setPendingDelete] = useState<ServiceRow | null>(null);

	const canManage = can(user, "service:create");
	const crossBusiness = businessId == null;

	const scoped = useMemo(
		() => (businesses ?? []).filter((b) => businessId == null || b.id === businessId),
		[businesses, businessId],
	);

	const rows: ServiceRow[] = useMemo(
		() =>
			scoped.flatMap((b) =>
				b.services.map((s) => ({ ...s, businessId: b.id, businessName: b.name })),
			),
		[scoped],
	);

	const filters = useMemo(
		() => [
			(r: ServiceRow) => businessFilter === "ALL" || String(r.businessId) === businessFilter,
			(r: ServiceRow) =>
				activeFilter === "ALL" ||
				(activeFilter === "ACTIVE" ? r.isActive : !r.isActive),
		],
		[businessFilter, activeFilter],
	);

	const table = useDataTable<ServiceRow>({
		rows,
		searchAccessor: (s) => `${s.name} ${s.description ?? ""} ${s.businessName}`,
		sortAccessor: (s, key) => {
			switch (key) {
				case "name": return s.name;
				case "business": return s.businessName;
				case "duration": return s.durationMinutes;
				case "price": return s.price;
				case "status": return s.isActive ? "active" : "inactive";
				default: return null;
			}
		},
		initialSort: { key: "name", direction: "asc" },
		filters,
		pageSize: 15,
	});

	/** Same-salon services only, so the duplicate guard doesn't compare across salons. */
	const siblingsFor = (targetBusinessId: Id | null) =>
		targetBusinessId == null ? [] : rows.filter((r) => r.businessId === targetBusinessId);

	async function handleSubmit(input: SalonServiceInput, chosenBusinessId: Id) {
		const target = editing?.businessId ?? businessId ?? chosenBusinessId;
		try {
			if (editing) {
				await update.mutateAsync({ businessId: target, id: editing.id, input });
				toast({ tone: "success", title: "Service updated", description: input.name });
			} else {
				await create.mutateAsync({ businessId: target, input });
				toast({ tone: "success", title: "Service added", description: input.name });
			}
			setFormOpen(false);
			setEditing(null);
		} catch (err) {
			toast({ tone: "error", title: "Couldn't save service", description: getErrorMessage(err) });
		}
	}

	async function handleDelete() {
		if (!pendingDelete) return;
		try {
			await remove.mutateAsync({ businessId: pendingDelete.businessId, id: pendingDelete.id });
			toast({ tone: "success", title: "Service deleted", description: pendingDelete.name });
			setPendingDelete(null);
		} catch (err) {
			toast({ tone: "error", title: "Couldn't delete service", description: getErrorMessage(err) });
		}
	}

	const columns: Column<ServiceRow>[] = [
		{
			key: "name",
			header: "Service",
			sortable: true,
			render: (s) => (
				<div className="min-w-0">
					<p className="font-medium text-ink">{s.name}</p>
					{s.description && <p className="truncate text-xs text-ink-subtle">{s.description}</p>}
				</div>
			),
		},
		...(crossBusiness
			? [
					{
						key: "business",
						header: "Salon",
						sortable: true,
						render: (s: ServiceRow) => (
							<Link
								href={`/businesses/${s.businessId}`}
								className="text-ink-muted transition-colors hover:text-primary-700 dark:hover:text-primary-300"
							>
								{s.businessName}
							</Link>
						),
					} satisfies Column<ServiceRow>,
			  ]
			: []),
		{
			key: "duration",
			header: "Duration",
			sortable: true,
			render: (s) => <span className="tabular-nums">{s.durationMinutes} min</span>,
		},
		{
			key: "price",
			header: "Price",
			sortable: true,
			align: "right",
			render: (s) => <span className="tabular-nums">{money(s.price)}</span>,
		},
		{
			key: "status",
			header: "Status",
			sortable: true,
			render: (s) => (
				<StatusBadge tone={s.isActive ? "success" : "neutral"}>
					{s.isActive ? "Bookable" : "Inactive"}
				</StatusBadge>
			),
		},
		...(canManage
			? [
					{
						key: "actions",
						header: "",
						align: "right",
						render: (s: ServiceRow) => (
							<div className="flex justify-end gap-1">
								<Button
									size="sm"
									variant="ghost"
									aria-label={`Edit ${s.name}`}
									onClick={() => {
										setEditing(s);
										setFormOpen(true);
									}}
									startIcon={<PencilIcon className="size-4" />}
								>
									Edit
								</Button>
								<Button
									size="sm"
									variant="ghost"
									aria-label={`Delete ${s.name}`}
									onClick={() => setPendingDelete(s)}
									startIcon={<TrashBinIcon className="size-4" />}
									className="text-error-600 hover:bg-error-50 hover:text-error-700 dark:hover:bg-error-500/10"
								>
									Delete
								</Button>
							</div>
						),
					} satisfies Column<ServiceRow>,
			  ]
			: []),
	];

	if (isError) {
		return (
			<EmptyState
				icon={<ErrorIcon className="size-6" />}
				title="Couldn't load services"
				description={getErrorMessage(error)}
				action={<Button variant="outline" onClick={() => refetch()}>Try again</Button>}
			/>
		);
	}

	return (
		<>
			<div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
				<SearchInput
					value={table.search}
					onChange={table.setSearch}
					placeholder="Search services…"
					aria-label="Search services"
					className="sm:max-w-xs"
				/>

				{crossBusiness && (
					<SelectInput
						aria-label="Filter by salon"
						value={businessFilter}
						onChange={(e) => setBusinessFilter(e.target.value)}
						className="sm:w-56"
					>
						<option value="ALL">All salons</option>
						{scoped.map((b) => (
							<option key={b.id} value={b.id}>
								{b.name}
							</option>
						))}
					</SelectInput>
				)}

				<SelectInput
					aria-label="Filter by availability"
					value={activeFilter}
					onChange={(e) => setActiveFilter(e.target.value as typeof activeFilter)}
					className="sm:w-44"
				>
					<option value="ALL">All services</option>
					<option value="ACTIVE">Bookable</option>
					<option value="INACTIVE">Inactive</option>
				</SelectInput>

				<div className="flex items-center gap-3 sm:ml-auto">
					{!isPending && (
						<p className="text-sm text-ink-subtle">
							{table.total} {table.total === 1 ? "service" : "services"}
						</p>
					)}
					{canManage && (
						<Button
							startIcon={<PlusIcon className="size-4" />}
							onClick={() => {
								setEditing(null);
								setFormOpen(true);
							}}
						>
							New service
						</Button>
					)}
				</div>
			</div>

			<DataTable
				columns={columns}
				rows={table.rows}
				rowKey={(s) => `${s.businessId}-${s.id}`}
				loading={isPending}
				sort={table.sort}
				onSort={table.toggleSort}
				empty={
					<EmptyState
						icon={<ListIcon className="size-6" />}
						title={table.isFiltered ? "No services match" : "No services yet"}
						description={
							table.isFiltered
								? "Try a different search term, or clear the filters."
								: "Add the treatments this salon offers so they can be booked."
						}
						action={
							table.isFiltered ? (
								<Button
									variant="outline"
									onClick={() => {
										table.setSearch("");
										setBusinessFilter("ALL");
										setActiveFilter("ALL");
									}}
								>
									Clear filters
								</Button>
							) : (
								canManage && (
									<Button
										startIcon={<PlusIcon className="size-4" />}
										onClick={() => {
											setEditing(null);
											setFormOpen(true);
										}}
									>
										New service
									</Button>
								)
							)
						}
					/>
				}
			/>

			{!isPending && table.total > 0 && (
				<div className="mt-px rounded-b-card border border-t-0 border-border-default bg-surface-raised">
					<Pagination
						page={table.page}
						pageCount={table.pageCount}
						total={table.total}
						pageSize={table.pageSize}
						onPageChange={table.setPage}
					/>
				</div>
			)}

			{/* Mounted only while open — the form resets by remounting. */}
			{formOpen && (
				<ServiceFormModal
					isOpen
					onClose={() => {
						setFormOpen(false);
						setEditing(null);
					}}
					onSubmit={handleSubmit}
					submitting={create.isPending || update.isPending}
					initial={editing}
					businessId={editing?.businessId ?? businessId}
					businessOptions={crossBusiness ? scoped.map((b) => ({ id: b.id, name: b.name })) : undefined}
					existing={siblingsFor(editing?.businessId ?? businessId)}
				/>
			)}

			<ConfirmDialog
				isOpen={pendingDelete !== null}
				onClose={() => setPendingDelete(null)}
				onConfirm={handleDelete}
				loading={remove.isPending}
				title={`Delete ${pendingDelete?.name ?? "this service"}?`}
				confirmLabel="Delete service"
				description={
					pendingDelete ? (
						<>
							This removes <strong className="text-ink">{pendingDelete.name}</strong> from{" "}
							<strong className="text-ink">{pendingDelete.businessName}</strong>. Clients will no
							longer be able to book it. This can&apos;t be undone.
						</>
					) : null
				}
			/>
		</>
	);
}
