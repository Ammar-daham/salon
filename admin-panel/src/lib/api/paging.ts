import { apiClient } from "./client";
import type { Page, PageQuery } from "./types";

/** A page as the API sends it. */
export interface PageDto<T> {
	items: T[];
	page: number;
	size: number;
	total_items: number;
	total_pages: number;
}

/** The most rows the API sends in one page. */
export const MAX_PAGE_SIZE = 100;

export function toPage<TDto, T>(dto: PageDto<TDto>, map: (item: TDto) => T): Page<T> {
	return {
		items: dto.items.map(map),
		page: dto.page,
		size: dto.size,
		totalItems: dto.total_items,
		totalPages: dto.total_pages,
	};
}

/**
 * The query string for a page: a blank search and unset filters are left out, rather than sent
 * empty, so the server doesn't filter on them. Filters come in under their API names.
 */
export function pageParams(query: PageQuery, filters: Record<string, string | number | null | undefined> = {}) {
	const params: Record<string, string | number> = {};
	const q = query.q?.trim();
	if (q) params.q = q;
	if (query.sort) params.sort = query.sort;
	if (query.page != null) params.page = query.page;
	if (query.size != null) params.size = query.size;
	for (const [name, value] of Object.entries(filters)) {
		if (value != null && value !== "") params[name] = value;
	}
	return params;
}

/**
 * Every row of a paged list, a full page at a time. Only for lists the platform keeps short, such as
 * its salons for a picker; anything that grows with use is paged in the UI instead.
 */
export async function fetchAllPages<TDto>(
	url: string,
	params: Record<string, string | number> = {},
	signal?: AbortSignal,
): Promise<TDto[]> {
	const rows: TDto[] = [];
	for (let page = 1; ; page++) {
		const { data } = await apiClient.get<PageDto<TDto>>(url, {
			params: { ...params, page, size: MAX_PAGE_SIZE },
			signal,
		});
		rows.push(...data.items);
		if (page >= data.total_pages) return rows;
	}
}
