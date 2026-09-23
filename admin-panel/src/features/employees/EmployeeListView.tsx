"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { can } from "@/lib/auth/permissions";
import { resolveBusinessScope } from "@/lib/auth/scope";
import { useDataTable } from "@/lib/table/useDataTable";
import { useEmployees } from "@/lib/resources/employees/employees.hooks";
import type { Employee } from "@/lib/resources/employees/employees.types";
import { getErrorMessage } from "@/lib/api/errors";

import PageHeader from "@/components/ui/PageHeader";
import DataTable, { type Column } from "@/components/ui/DataTable";
import SearchInput from "@/components/ui/SearchInput";
import Pagination from "@/components/ui/Pagination";
import EmptyState from "@/components/ui/EmptyState";
import StatusBadge from "@/components/ui/StatusBadge";
import Button from "@/components/ui/button/Button";
import InitialsAvatar from "@/components/ui/InitialsAvatar";
import { SelectInput } from "@/components/ui/form/Field";
import { ErrorIcon, PlusIcon, UserCircleIcon, UserIcon } from "@/icons";

export default function EmployeeListView() {
	const { user } = useAuth();
	const router = useRouter();
	const scope = resolveBusinessScope(user);

	const businessId = scope.kind === "business" ? scope.businessId : null;
	const { data, isPending, isError, error, refetch, source } = useEmployees(businessId);

	const [statusFilter, setStatusFilter] = useState<"ALL" | "ACTIVE" | "INACTIVE">("ALL");
	const [businessFilter, setBusinessFilter] = useState<string>("ALL");

	const canCreate = can(user, "employee:create");
	const crossBusiness = scope.kind === "platform";

	const salons = useMemo(() => {
		const seen = new Map<number, string>();
		data.forEach((e) => seen.set(Number(e.businessId), e.businessName));
		return [...seen.entries()].map(([id, name]) => ({ id, name }));
	}, [data]);

	const filters = useMemo(
		() => [
			(e: Employee) =>
				statusFilter === "ALL" || (statusFilter === "ACTIVE" ? e.isActive : !e.isActive),
			(e: Employee) => businessFilter === "ALL" || String(e.businessId) === businessFilter,
		],
		[statusFilter, businessFilter],
	);

	const table = useDataTable<Employee>({
		rows: data,
		searchAccessor: (e) => `${e.firstName} ${e.lastName} ${e.email} ${e.title} ${e.businessName}`,
		sortAccessor: (e, key) => {
			switch (key) {
				case "name": return `${e.firstName} ${e.lastName}`;
				case "title": return e.title;
				case "business": return e.businessName;
				case "hiredAt": return e.hiredAt;
				case "status": return e.isActive ? "active" : "inactive";
				default: return null;
			}
		},
		initialSort: { key: "name", direction: "asc" },
		filters,
		pageSize: 15,
	});

	if (scope.kind === "unresolved") {
		return (
			<>
				<PageHeader title="Employees" />
				<EmptyState
					icon={<UserIcon className="size-6" />}
					title="Your account isn't linked to a salon yet"
					description="Ask a platform administrator to attach your account to a business. Your team will appear here once linked."
				/>
			</>
		);
	}

	const columns: Column<Employee>[] = [
		{
			key: "name",
			header: "Name",
			sortable: true,
			render: (e) => (
				<div className="flex items-center gap-3">
					<InitialsAvatar firstName={e.firstName} lastName={e.lastName} size="sm" />
					<div className="min-w-0">
						<Link
							href={`/employees/${e.id}`}
							className="font-medium text-ink hover:text-primary-700 dark:hover:text-primary-300"
						>
							{e.firstName} {e.lastName}
						</Link>
						<p className="truncate text-xs text-ink-subtle">{e.email}</p>
					</div>
				</div>
			),
		},
		{ key: "title", header: "Role", sortable: true, render: (e) => e.title },
		...(crossBusiness
			? [
					{
						key: "business",
						header: "Salon",
						sortable: true,
						render: (e: Employee) => (
							<Link
								href={`/businesses/${e.businessId}`}
								className="text-ink-muted transition-colors hover:text-primary-700 dark:hover:text-primary-300"
							>
								{e.businessName}
							</Link>
						),
					} satisfies Column<Employee>,
			  ]
			: []),
		{
			key: "hiredAt",
			header: "Joined",
			sortable: true,
			render: (e) => (
				<span className="text-ink-muted">
					{new Date(e.hiredAt).toLocaleDateString(undefined, {
						year: "numeric",
						month: "short",
					})}
				</span>
			),
		},
		{
			key: "status",
			header: "Status",
			sortable: true,
			render: (e) => (
				<StatusBadge tone={e.isActive ? "success" : "neutral"}>
					{e.isActive ? "Active" : "Inactive"}
				</StatusBadge>
			),
		},
		{
			key: "actions",
			header: "",
			align: "right",
			render: (e) => (
				<Button size="sm" variant="outline" onClick={() => router.push(`/employees/${e.id}`)}>
					View
				</Button>
			),
		},
	];

	return (
		<>
			<PageHeader
				title="Employees"
				description={
					crossBusiness
						? "Staff across every salon on the platform."
						: "Your salon's team, their titles and their schedules."
				}
				sampleData={source === "mock"}
				actions={
					canCreate && (
						<Button
							startIcon={<PlusIcon className="size-4" />}
							onClick={() => router.push("/employees/new")}
						>
							Add employee
						</Button>
					)
				}
			/>

			{isError ? (
				<EmptyState
					icon={<ErrorIcon className="size-6" />}
					title="Couldn't load the team"
					description={getErrorMessage(error)}
					action={<Button variant="outline" onClick={() => refetch()}>Try again</Button>}
				/>
			) : (
				<>
					<div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
						<SearchInput
							value={table.search}
							onChange={table.setSearch}
							placeholder="Search by name, email or role…"
							aria-label="Search employees"
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
						<SelectInput
							aria-label="Filter by status"
							value={statusFilter}
							onChange={(e) => setStatusFilter(e.target.value as typeof statusFilter)}
							className="sm:w-44"
						>
							<option value="ALL">All staff</option>
							<option value="ACTIVE">Active</option>
							<option value="INACTIVE">Inactive</option>
						</SelectInput>
						{!isPending && (
							<p className="text-sm text-ink-subtle sm:ml-auto">
								{table.total} {table.total === 1 ? "person" : "people"}
							</p>
						)}
					</div>

					<DataTable
						columns={columns}
						rows={table.rows}
						rowKey={(e) => e.id}
						loading={isPending}
						sort={table.sort}
						onSort={table.toggleSort}
						empty={
							<EmptyState
								icon={<UserCircleIcon className="size-6" />}
								title={table.isFiltered ? "No one matches" : "No staff yet"}
								description={
									table.isFiltered
										? "Try a different search term, or clear the filters."
										: "Add your first team member to start assigning bookings."
								}
								action={
									table.isFiltered ? (
										<Button
											variant="outline"
											onClick={() => {
												table.setSearch("");
												setStatusFilter("ALL");
												setBusinessFilter("ALL");
											}}
										>
											Clear filters
										</Button>
									) : (
										canCreate && (
											<Button
												startIcon={<PlusIcon className="size-4" />}
												onClick={() => router.push("/employees/new")}
											>
												Add employee
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
				</>
			)}
		</>
	);
}
