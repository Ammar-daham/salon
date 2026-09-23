"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { resolveBusinessScope } from "@/lib/auth/scope";
import { useDataTable } from "@/lib/table/useDataTable";
import { useCustomers } from "@/lib/resources/customers/customers.hooks";
import type { Customer } from "@/lib/resources/customers/customers.types";
import { getErrorMessage } from "@/lib/api/errors";

import PageHeader from "@/components/ui/PageHeader";
import DataTable, { type Column } from "@/components/ui/DataTable";
import SearchInput from "@/components/ui/SearchInput";
import Pagination from "@/components/ui/Pagination";
import EmptyState from "@/components/ui/EmptyState";
import Button from "@/components/ui/button/Button";
import InitialsAvatar from "@/components/ui/InitialsAvatar";
import { SelectInput } from "@/components/ui/form/Field";
import { ErrorIcon, UserIcon } from "@/icons";

function money(value: number) {
	return new Intl.NumberFormat(undefined, {
		style: "currency",
		currency: "EUR",
		maximumFractionDigits: 0,
	}).format(value);
}

function relativeDays(iso: string) {
	const days = Math.floor((Date.now() - new Date(iso).getTime()) / 86_400_000);
	if (days <= 0) return "Today";
	if (days === 1) return "Yesterday";
	if (days < 30) return `${days} days ago`;
	if (days < 365) return `${Math.floor(days / 30)} mo ago`;
	return `${Math.floor(days / 365)} yr ago`;
}

export default function CustomerListView() {
	const { user } = useAuth();
	const router = useRouter();
	const scope = resolveBusinessScope(user);

	const businessId = scope.kind === "business" ? scope.businessId : null;
	const { data, isPending, isError, error, refetch, source } = useCustomers(businessId);

	const [businessFilter, setBusinessFilter] = useState<string>("ALL");
	const crossBusiness = scope.kind === "platform";

	const salons = useMemo(() => {
		const seen = new Map<number, string>();
		data.forEach((c) => seen.set(Number(c.businessId), c.businessName));
		return [...seen.entries()].map(([id, name]) => ({ id, name }));
	}, [data]);

	const filters = useMemo(
		() => [(c: Customer) => businessFilter === "ALL" || String(c.businessId) === businessFilter],
		[businessFilter],
	);

	const table = useDataTable<Customer>({
		rows: data,
		searchAccessor: (c) => `${c.firstName} ${c.lastName} ${c.email} ${c.phone} ${c.businessName}`,
		sortAccessor: (c, key) => {
			switch (key) {
				case "name": return `${c.firstName} ${c.lastName}`;
				case "business": return c.businessName;
				case "visits": return c.totalVisits;
				case "spend": return c.totalSpend;
				case "lastVisit": return c.lastVisit;
				default: return null;
			}
		},
		initialSort: { key: "lastVisit", direction: "desc" },
		filters,
		pageSize: 15,
	});

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
						<p className="truncate text-xs text-ink-subtle">{c.email}</p>
					</div>
				</div>
			),
		},
		...(crossBusiness
			? [
					{
						key: "business",
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
			key: "visits",
			header: "Visits",
			sortable: true,
			render: (c) => <span className="tabular-nums">{c.totalVisits}</span>,
		},
		{
			key: "spend",
			header: "Lifetime spend",
			sortable: true,
			align: "right",
			render: (c) => <span className="tabular-nums">{money(c.totalSpend)}</span>,
		},
		{
			key: "lastVisit",
			header: "Last visit",
			sortable: true,
			render: (c) => <span className="text-ink-muted">{relativeDays(c.lastVisit)}</span>,
		},
		{
			key: "actions",
			header: "",
			align: "right",
			render: (c) => (
				<Button size="sm" variant="outline" onClick={() => router.push(`/customers/${c.id}`)}>
					View
				</Button>
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
						: "Your salon's clients, their visit history and their notes."
				}
				sampleData={source === "mock"}
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
								onChange={(e) => setBusinessFilter(e.target.value)}
								className="sm:w-56"
							>
								<option value="ALL">All salons</option>
								{salons.map((s) => (
									<option key={s.id} value={s.id}>
										{s.name}
									</option>
								))}
							</SelectInput>
						)}
						{!isPending && (
							<p className="text-sm text-ink-subtle sm:ml-auto">
								{table.total} {table.total === 1 ? "client" : "clients"}
							</p>
						)}
					</div>

					<DataTable
						columns={columns}
						rows={table.rows}
						rowKey={(c) => c.id}
						loading={isPending}
						sort={table.sort}
						onSort={table.toggleSort}
						empty={
							<EmptyState
								icon={<UserIcon className="size-6" />}
								title={table.isFiltered ? "No clients match" : "No clients yet"}
								description={
									table.isFiltered
										? "Try a different search term, or clear the filters."
										: "Clients appear here once they book their first appointment."
								}
								action={
									table.isFiltered && (
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
				</>
			)}
		</>
	);
}
