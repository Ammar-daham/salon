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

	/**
	 * The backend uses the DELETE *body* to cascade child deletes —
	 * BusinessDataAccessService.deleteBusiness iterates the addresses, contacts
	 * and services it finds there. Sending `{}` orphans every child row, and
	 * because contacts.value is globally UNIQUE, an orphaned contact makes that
	 * phone number permanently unusable platform-wide.
	 *
	 * So: re-read the record and echo it back. Hidden here so no call site has
	 * to know.
	 */
	async remove(id: Id) {
		const { data } = await apiClient.get<BusinessDto>(endpoints.businesses.byId(id));
		await apiClient.delete(endpoints.businesses.byId(id), { data });
	},
};
