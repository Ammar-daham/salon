"use client";

import { useEffect, useState } from "react";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import type { Page, PageQuery } from "@/lib/api/types";
import type { SortState } from "./useDataTable";

/** The API's sort parameter for a column sort: the key, or "-key" for descending. */
export function sortParam(sort: SortState | null): string | undefined {
	if (!sort) return undefined;
	return sort.direction === "desc" ? `-${sort.key}` : sort.key;
}

interface UseServerTableOptions {
	/** Column keys are the API's sort keys, e.g. "created_at". */
	initialSort?: SortState;
	pageSize?: number;
}

/**
 * A table the server pages, searches and sorts (BE-15), as opposed to `useDataTable`, which does it
 * in the browser. It holds the page, the search box and the sort, and turns them into the query to
 * send. The search is sent once typing settles; a new search or sort starts again from page 1.
 */
export function useServerTable({ initialSort, pageSize = 20 }: UseServerTableOptions = {}) {
	const [page, setPage] = useState(1);
	const [search, setSearch] = useState("");
	const [sort, setSort] = useState<SortState | null>(initialSort ?? null);
	const settledSearch = useDebouncedValue(search.trim());

	const query: PageQuery = { page, size: pageSize, q: settledSearch || undefined, sort: sortParam(sort) };

	return {
		query,
		page,
		pageSize,
		search,
		sort,
		setPage,
		setSearch: (value: string) => {
			setSearch(value);
			setPage(1);
		},
		toggleSort: (key: string) => {
			setSort((current) =>
				current?.key === key
					? { key, direction: current.direction === "asc" ? "desc" : "asc" }
					: { key, direction: "asc" },
			);
			setPage(1);
		},
		/** For a filter outside the table changing: its first page is the one to show. */
		firstPage: () => setPage(1),
		isSearching: search.trim().length > 0,
	};
}

/**
 * Steps back when the page shown has emptied, e.g. after deleting the only row on the last page,
 * rather than showing an empty page of a list that isn't empty.
 */
export function useStayOnAPage(table: { page: number; setPage: (page: number) => void }, data: Page<unknown> | undefined) {
	const { page, setPage } = table;
	useEffect(() => {
		if (data && data.items.length === 0 && page > 1) setPage(Math.max(1, data.totalPages));
	}, [data, page, setPage]);
}
