"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { can } from "@/lib/auth/permissions";
import { resolveBusinessScope } from "@/lib/auth/scope";
import { useServerTable, useStayOnAPage } from "@/lib/table/useServerTable";
import { useBusinesses } from "@/lib/resources/businesses/businesses.hooks";
import { useCustomerPage, useDeleteCustomer } from "@/lib/resources/customers/customers.hooks";
import type { Customer } from "@/lib/resources/customers/customers.types";
import { getErrorMessage } from "@/lib/api/errors";

import PageHeader from "@/components/ui/PageHeader";
import DataTable, { type Column } from "@/components/ui/DataTable";
import SearchInput from "@/components/ui/SearchInput";
import Pagination from "@/components/ui/Pagination";
import EmptyState from "@/components/ui/EmptyState";
import Button from "@/components/ui/button/Button";
import InitialsAvatar from "@/components/ui/InitialsAvatar";
import ConfirmDialog from "@/components/ui/modal/ConfirmDialog";
import { SelectInput } from "@/components/ui/form/Field";
import { useToast } from "@/components/ui/toast/ToastProvider";
import { ErrorIcon, PlusIcon, TrashBinIcon, UserIcon } from "@/icons";

export default function CustomerListView() {
	const { user } = useAuth();
	const router = useRouter();
	const { toast } = useToast();
	const scope = resolveBusinessScope(user);

	const [businessFilter, setBusinessFilter] = useState<string>("ALL");
	const [pendingDelete, setPendingDelete] = useState<Customer | null>(null);

	const canCreate = can(user, "customer:create");
	const canDelete = can(user, "customer:delete");
	const crossBusiness = scope.kind === "platform";

	// Searched, filtered, sorted and paged by the server, across salons for a platform caller (BE-15).
	const table = useServerTable({ initialSort: { key: "name", direction: "asc" }, pageSize: 15 });
	const { data, isPending, isError, error, refetch, source } = useCustomerPage(scope, {
		...table.query,
		businessId: businessFilter === "ALL" ? null : Number(businessFilter),
	});
	useStayOnAPage(table, data);
	const { data: salons } = useBusinesses(crossBusiness);
	const remove = useDeleteCustomer();

	const isFiltered = table.isSearching || businessFilter !== "ALL";
	const total = data?.totalItems ?? 0;

	async function handleDelete() {
		if (!pendingDelete) return;
		const target = pendingDelete;
		try {
			await remove.mutateAsync({ businessId: target.businessId, id: target.id });
			toast({
				tone: "success",
				title: "Client deleted",
				description: `${target.firstName} ${target.lastName} has been removed from ${target.businessName}'s client list.`,
			});
			setPendingDelete(null);
		} catch (err) {
			toast({ tone: "error", title: "Couldn't delete client", description: getErrorMessage(err) });
		}
	}

	if (scope.kind === "unresolved") {
		return (
			<>
				<PageHeader title="Customers" />
				<EmptyState
					icon={<UserIcon className="size-6" />}
					title="Your account isn't linked to a salon yet"
					description="Ask a platform administrator to attach your account to a business. Your clients will appear here once linked."
				/>
			</>
		);
	}

	const columns: Column<Customer>[] = [
		{
			key: "name",
			header: "Client",
			sortable: true,
			render: (c) => (
				<div className="flex items-center gap-3">
					<InitialsAvatar firstName={c.firstName} lastName={c.lastName} size="sm" muted />
					<div className="min-w-0">
						<Link
							href={`/customers/${c.id}`}
							className="font-medium text-ink hover:text-primary-700 dark:hover:text-primary-300"
						>
							{c.firstName} {c.lastName}
						</Link>
						{c.email && <p className="truncate text-xs text-ink-subtle">{c.email}</p>}
					</div>
				</div>
			),
		},
		...(crossBusiness
			? [
					{
						key: "business_name",
						header: "Salon",
						sortable: true,
						render: (c: Customer) => (
							<Link
								href={`/businesses/${c.businessId}`}
								className="text-ink-muted transition-colors hover:text-primary-700 dark:hover:text-primary-300"
							>
								{c.businessName}
							</Link>
						),
					} satisfies Column<Customer>,
			  ]
			: []),
		{
			key: "phone",
			header: "Phone",
			render: (c) => <span className="tabular-nums text-ink-muted">{c.phone ?? "—"}</span>,
		},
		{
			key: "created_at",
			header: "Client since",
			sortable: true,
			render: (c) => (
				<span className="text-ink-muted">
					{new Date(c.createdAt).toLocaleDateString(undefined, { year: "numeric", month: "short" })}
				</span>
			),
		},
		{
			key: "actions",
			header: "",
			align: "right",
			render: (c) => (
				<div className="flex justify-end gap-1">
					<Button size="sm" variant="outline" onClick={() => router.push(`/customers/${c.id}`)}>
						View
					</Button>
					{canDelete && (
						<Button
							size="sm"
							variant="ghost"
							aria-label={`Delete ${c.firstName} ${c.lastName}`}
							onClick={() => setPendingDelete(c)}
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
				title="Customers"
				description={
					crossBusiness
						? "Clients across every salon on the platform."
						: "Your salon's clients, their contact details and their notes."
				}
				sampleData={source === "mock"}
				actions={
					canCreate && (
						<Button
							startIcon={<PlusIcon className="size-4" />}
							onClick={() => router.push("/customers/new")}
						>
							Add client
						</Button>
					)
				}
			/>

			{isError ? (
				<EmptyState
					icon={<ErrorIcon className="size-6" />}
					title="Couldn't load clients"
					description={getErrorMessage(error)}
					action={<Button variant="outline" onClick={() => refetch()}>Try again</Button>}
				/>
			) : (
				<>
					<div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
						<SearchInput
							value={table.search}
							onChange={table.setSearch}
							placeholder="Search by name, email or phone…"
							aria-label="Search customers"
							className="sm:max-w-xs"
						/>
						{crossBusiness && (
							<SelectInput
								aria-label="Filter by salon"
								value={businessFilter}
								onChange={(e) => {
									setBusinessFilter(e.target.value);
									table.firstPage();
								}}
								className="sm:w-56"
							>
								<option value="ALL">All salons</option>
								{(salons ?? []).map((s) => (
									<option key={s.id} value={s.id}>
										{s.name}
									</option>
								))}
							</SelectInput>
						)}
						{!isPending && (
							<p className="text-sm text-ink-subtle sm:ml-auto">
								{total} {total === 1 ? "client" : "clients"}
							</p>
						)}
					</div>

					<DataTable
						columns={columns}
						rows={data?.items ?? []}
						rowKey={(c) => c.id}
						loading={isPending}
						sort={table.sort}
						onSort={table.toggleSort}
						empty={
							<EmptyState
								icon={<UserIcon className="size-6" />}
								title={isFiltered ? "No clients match" : "No clients yet"}
								description={
									isFiltered
										? "Try a different search term, or clear the filters."
										: canCreate
											? "Add your first client to start building your list."
											: "Clients your salon adds will appear here."
								}
								action={
									isFiltered && (
										<Button
											variant="outline"
											onClick={() => {
												table.setSearch("");
												setBusinessFilter("ALL");
											}}
										>
											Clear filters
										</Button>
									)
								}
							/>
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
				title={`Delete ${pendingDelete?.firstName ?? ""} ${pendingDelete?.lastName ?? ""}?`.trim()}
				confirmLabel="Delete client"
				description={
					pendingDelete ? (
						<>
							This permanently deletes{" "}
							<strong className="text-ink">
								{pendingDelete.firstName} {pendingDelete.lastName}
							</strong>
							&apos;s record from {pendingDelete.businessName}, including their contact details
							and notes. This can&apos;t be undone.
						</>
					) : null
				}
			/>
		</>
	);
}
