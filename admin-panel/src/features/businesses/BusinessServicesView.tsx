"use client";

import { useParams } from "next/navigation";
import { useBusiness } from "@/lib/resources/businesses/businesses.hooks";
import { useDataTable } from "@/lib/table/useDataTable";
import type { SalonService } from "@/lib/resources/businesses/businesses.types";
import DataTable, { type Column } from "@/components/ui/DataTable";
import SearchInput from "@/components/ui/SearchInput";
import EmptyState from "@/components/ui/EmptyState";
import StatusBadge from "@/components/ui/StatusBadge";
import { ListIcon } from "@/icons";

function money(value: number) {
	return new Intl.NumberFormat(undefined, { style: "currency", currency: "EUR" }).format(value);
}

export default function BusinessServicesView() {
	const params = useParams<{ id: string }>();
	const id = Number(params.id);
	const { data: business, isPending } = useBusiness(Number.isNaN(id) ? null : id);

	/**
	 * The services list is a projection of the business response — the backend
	 * has no list endpoint for services, so this reads from the same cache entry
	 * rather than issuing its own request.
	 */
	const table = useDataTable<SalonService>({
		rows: business?.services ?? [],
		searchAccessor: (s) => `${s.name} ${s.description ?? ""}`,
		sortAccessor: (s, key) => {
			switch (key) {
				case "name": return s.name;
				case "duration": return s.durationMinutes;
				case "price": return s.price;
				case "status": return s.isActive ? "active" : "inactive";
				default: return null;
			}
		},
		initialSort: { key: "name", direction: "asc" },
		pageSize: 25,
	});

	const columns: Column<SalonService>[] = [
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
					{s.isActive ? "Active" : "Inactive"}
				</StatusBadge>
			),
		},
	];

	return (
		<>
			<div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
				<SearchInput
					value={table.search}
					onChange={table.setSearch}
					placeholder="Search services…"
					aria-label="Search services"
					className="sm:max-w-sm"
				/>
				{!isPending && (
					<p className="text-sm text-ink-subtle sm:ml-auto">
						{table.total} {table.total === 1 ? "service" : "services"}
					</p>
				)}
			</div>

			<DataTable
				columns={columns}
				rows={table.rows}
				rowKey={(s) => s.id}
				loading={isPending}
				sort={table.sort}
				onSort={table.toggleSort}
				empty={
					<EmptyState
						icon={<ListIcon className="size-6" />}
						title={table.isFiltered ? "No services match" : "No services yet"}
						description={
							table.isFiltered
								? "Try a different search term."
								: "Service management — adding, editing and pricing — arrives in Phase 2."
						}
					/>
				}
			/>
		</>
	);
}
