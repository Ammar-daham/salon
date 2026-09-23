"use client";

import { useMemo, useState } from "react";

export type SortDirection = "asc" | "desc";

export interface SortState<TKey extends string = string> {
	key: TKey;
	direction: SortDirection;
}

interface UseDataTableOptions<T> {
	rows: T[];
	/** Fields concatenated for the free-text search box. */
	searchAccessor?: (row: T) => string;
	/** Value used when sorting by a given column key. */
	sortAccessor?: (row: T, key: string) => string | number | null | undefined;
	initialSort?: SortState;
	pageSize?: number;
	/** Extra predicates from the filter bar, ANDed together. */
	filters?: Array<(row: T) => boolean>;
}

/**
 * Search, sort and pagination all happen client-side — the backend exposes no
 * query parameters on any list endpoint, so there is nothing to delegate to.
 * Collections are small enough (one salon's staff, one platform's businesses)
 * that this is correct rather than merely expedient.
 */
export function useDataTable<T>({
	rows,
	searchAccessor,
	sortAccessor,
	initialSort,
	pageSize = 10,
	filters = [],
}: UseDataTableOptions<T>) {
	const [search, setSearch] = useState("");
	const [sort, setSort] = useState<SortState | null>(initialSort ?? null);
	const [page, setPage] = useState(1);

	const filtered = useMemo(() => {
		const term = search.trim().toLowerCase();
		return rows.filter((row) => {
			if (term && searchAccessor && !searchAccessor(row).toLowerCase().includes(term)) {
				return false;
			}
			return filters.every((predicate) => predicate(row));
		});
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [rows, search, searchAccessor, ...filters]);

	const sorted = useMemo(() => {
		if (!sort || !sortAccessor) return filtered;
		const factor = sort.direction === "asc" ? 1 : -1;
		return [...filtered].sort((a, b) => {
			const av = sortAccessor(a, sort.key);
			const bv = sortAccessor(b, sort.key);
			if (av == null && bv == null) return 0;
			if (av == null) return 1; // nulls always last, both directions
			if (bv == null) return -1;
			if (typeof av === "number" && typeof bv === "number") return (av - bv) * factor;
			return String(av).localeCompare(String(bv), undefined, { numeric: true }) * factor;
		});
	}, [filtered, sort, sortAccessor]);

	const total = sorted.length;
	const pageCount = Math.max(1, Math.ceil(total / pageSize));
	const safePage = Math.min(page, pageCount);
	const paged = useMemo(
		() => sorted.slice((safePage - 1) * pageSize, safePage * pageSize),
		[sorted, safePage, pageSize],
	);

	function toggleSort(key: string) {
		setSort((current) =>
			current?.key === key
				? { key, direction: current.direction === "asc" ? "desc" : "asc" }
				: { key, direction: "asc" },
		);
		setPage(1);
	}

	return {
		rows: paged,
		allFilteredRows: sorted,
		total,
		page: safePage,
		pageCount,
		pageSize,
		search,
		sort,
		setSearch: (value: string) => {
			setSearch(value);
			setPage(1);
		},
		setPage,
		toggleSort,
		isEmpty: total === 0,
		isFiltered: search.trim().length > 0 || filters.length > 0,
	};
}
