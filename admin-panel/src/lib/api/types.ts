export type Id = number;

/** Where a resource's data actually comes from. Surfaced in the UI as a
 *  "Sample data" pill so the real/mock boundary is auditable at a glance. */
export type DataSource = "live" | "mock";

export interface ListParams {
	signal?: AbortSignal;
}

/**
 * One shape for every resource, whether it is backed by the real API or by the
 * mock store. Swapping a mock for the real thing is a one-line change in
 * `src/lib/resources/index.ts` and nothing at the call sites moves.
 *
 * Deliberately no pagination/sort/filter arguments: the backend has none on any
 * endpoint, so that work happens client-side in `useDataTable`. `ListParams`
 * exists as the forward-compatible seam for when the backend grows them.
 */
export interface Repository<TEntity, TCreate = unknown, TUpdate = TCreate> {
	readonly source: DataSource;
	list(params?: ListParams): Promise<TEntity[]>;
	get(id: Id, params?: ListParams): Promise<TEntity>;
	create(input: TCreate): Promise<TEntity>;
	update(id: Id, input: TUpdate): Promise<void>;
	remove(id: Id): Promise<void>;
}
