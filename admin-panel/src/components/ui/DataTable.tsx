"use client";

import React from "react";
import { cn } from "@/lib/utils/cn";
import { TableSkeleton } from "./Skeleton";
import type { SortState } from "@/lib/table/useDataTable";
import { ChevronDownIcon } from "@/icons";

export interface Column<T> {
	key: string;
	header: string;
	/** Omit to render nothing sortable for this column. */
	sortable?: boolean;
	render: (row: T) => React.ReactNode;
	className?: string;
	headerClassName?: string;
	/** Right-aligned action columns shouldn't announce as sortable. */
	align?: "left" | "right";
}

interface DataTableProps<T> {
	columns: Column<T>[];
	rows: T[];
	rowKey: (row: T) => string | number;
	loading?: boolean;
	sort?: SortState | null;
	onSort?: (key: string) => void;
	empty?: React.ReactNode;
	onRowClick?: (row: T) => void;
	className?: string;
}

export default function DataTable<T>({
	columns,
	rows,
	rowKey,
	loading = false,
	sort,
	onSort,
	empty,
	onRowClick,
	className,
}: DataTableProps<T>) {
	if (loading) {
		return (
			<div className={cn("overflow-hidden rounded-card border border-border-default bg-surface-raised", className)}>
				<TableSkeleton rows={5} columns={columns.length} />
			</div>
		);
	}

	if (rows.length === 0 && empty) {
		return <>{empty}</>;
	}

	return (
		<div
			className={cn(
				"overflow-hidden rounded-card border border-border-default bg-surface-raised",
				className,
			)}
		>
			{/* Only the table scrolls sideways, never the page body. */}
			<div className="overflow-x-auto">
				<table className="w-full min-w-[640px] border-collapse text-left">
					<thead>
						<tr className="border-b border-border-default bg-surface-sunken">
							{columns.map((col) => {
								const isSorted = sort?.key === col.key;
								return (
									<th
										key={col.key}
										scope="col"
										aria-sort={
											col.sortable
												? isSorted
													? sort.direction === "asc"
														? "ascending"
														: "descending"
													: "none"
												: undefined
										}
										className={cn(
											"px-5 py-3 text-xs font-medium uppercase tracking-wide text-ink-subtle",
											col.align === "right" && "text-right",
											col.headerClassName,
										)}
									>
										{col.sortable && onSort ? (
											<button
												type="button"
												onClick={() => onSort(col.key)}
												className="inline-flex items-center gap-1 uppercase transition-colors hover:text-ink"
											>
												{col.header}
												<ChevronDownIcon
													className={cn(
														"size-4 transition-transform",
														isSorted ? "text-primary-600" : "opacity-40",
														isSorted && sort.direction === "asc" && "rotate-180",
													)}
												/>
											</button>
										) : (
											col.header
										)}
									</th>
								);
							})}
						</tr>
					</thead>
					<tbody className="divide-y divide-border-default">
						{rows.map((row) => (
							<tr
								key={rowKey(row)}
								onClick={onRowClick ? () => onRowClick(row) : undefined}
								className={cn(
									"transition-colors",
									onRowClick && "cursor-pointer hover:bg-primary-50/60 dark:hover:bg-primary-500/[0.07]",
								)}
							>
								{columns.map((col) => (
									<td
										key={col.key}
										className={cn(
											"px-5 py-4 text-sm text-ink",
											col.align === "right" && "text-right",
											col.className,
										)}
									>
										{col.render(row)}
									</td>
								))}
							</tr>
						))}
					</tbody>
				</table>
			</div>
		</div>
	);
}
