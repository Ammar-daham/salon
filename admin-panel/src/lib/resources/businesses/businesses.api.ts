import { apiClient } from "@/lib/api/client";
import { endpoints } from "@/lib/api/endpoints";
import type { Id, ListParams, Repository } from "@/lib/api/types";
import type { Business, BusinessInput } from "./businesses.types";
import { toBusiness, toBusinessRequest, type BusinessDto } from "./businesses.mappers";

export const businessesRepository: Repository<Business, BusinessInput, BusinessInput> = {
	source: "live",

	async list(params?: ListParams) {
		const { data } = await apiClient.get<BusinessDto[]>(endpoints.businesses.root, {
			signal: params?.signal,
		});
		return (data ?? []).map(toBusiness);
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
