export type Id = number;

/** Where a resource's data actually comes from. Surfaced in the UI as a
 *  "Sample data" pill so the real/mock boundary is auditable at a glance. */
export type DataSource = "live" | "mock";

export interface ListParams {
	signal?: AbortSignal;
}

/** One page of a list (BE-15): its rows, and how long the whole list is. */
export interface Page<T> {
	items: T[];
	page: number;
	size: number;
	totalItems: number;
	totalPages: number;
}

/**
 * What every paged list takes. page counts from 1 and size is at most 100; q searches what the
 * endpoint searches; sort is one of its keys, with a leading "-" for the other way round.
 */
export interface PageQuery {
	page?: number;
	size?: number;
	q?: string;
	sort?: string;
}

/**
 * One shape for every resource, whether it is backed by the real API or by the
 * mock store. Swapping a mock for the real thing is a one-line change in
 * `src/lib/resources/index.ts` and nothing at the call sites moves.
 *
 * A list comes a page at a time, searched, filtered and sorted on the server (BE-15).
 */
export interface Repository<TEntity, TCreate = unknown, TUpdate = TCreate, TQuery extends PageQuery = PageQuery> {
	readonly source: DataSource;
	list(query: TQuery, params?: ListParams): Promise<Page<TEntity>>;
	get(id: Id, params?: ListParams): Promise<TEntity>;
	create(input: TCreate): Promise<TEntity>;
	update(id: Id, input: TUpdate): Promise<void>;
	remove(id: Id): Promise<void>;
}
