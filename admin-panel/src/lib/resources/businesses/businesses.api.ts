import { apiClient } from "@/lib/api/client";
import { endpoints } from "@/lib/api/endpoints";
import { fetchAllPages, pageParams, toPage, type PageDto } from "@/lib/api/paging";
import type { Id, ListParams, PageQuery, Repository } from "@/lib/api/types";
import type { Business, BusinessInput, BusinessStatus } from "./businesses.types";
import { toBusiness, toBusinessRequest, type BusinessDto } from "./businesses.mappers";

/** sort is name, status or created_at; q matches the name, description or a city. */
export interface BusinessPageQuery extends PageQuery {
	status?: BusinessStatus;
}

export const businessesRepository: Repository<Business, BusinessInput, BusinessInput, BusinessPageQuery> = {
	source: "live",

	async list(query: BusinessPageQuery, params?: ListParams) {
		const { data } = await apiClient.get<PageDto<BusinessDto>>(endpoints.businesses.root, {
			params: pageParams(query, { status: query.status }),
			signal: params?.signal,
		});
		return toPage(data, toBusiness);
	},

	async get(id: Id, params?: ListParams) {
		const { data } = await apiClient.get<BusinessDto>(endpoints.businesses.byId(id), {
			signal: params?.signal,
		});
		return toBusiness(data);
	},

	async create(input: BusinessInput) {
		const { data } = await apiClient.post<BusinessDto>(
			endpoints.businesses.root,
			toBusinessRequest(input),
		);
		return toBusiness(data);
	},

	async update(id: Id, input: BusinessInput) {
		await apiClient.put(endpoints.businesses.byId(id), toBusinessRequest(input));
	},

	async remove(id: Id) {
		await apiClient.delete(endpoints.businesses.byId(id));
	},
};

/**
 * Every salon the caller can see, by name: for pickers, and for reading something per salon across
 * the platform. A platform's salons stay few enough for that; its people and clients are paged instead.
 */
export async function listAllBusinesses(params?: ListParams): Promise<Business[]> {
	const rows = await fetchAllPages<BusinessDto>(endpoints.businesses.root, { sort: "name" }, params?.signal);
	return rows.map(toBusiness);
}
