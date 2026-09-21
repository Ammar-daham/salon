import { apiClient } from "@/lib/api/client";
import { endpoints } from "@/lib/api/endpoints";
import type { Id, ListParams, Repository } from "@/lib/api/types";
import type { Business, BusinessInput } from "./businesses.types";

/**
 * Wire shape. Jackson serialises FIELD names (camelCase) regardless of the
 * @JsonProperty annotations on constructor parameters — those only affect
 * deserialisation. So responses are camelCase even where request bodies are
 * snake_case. Any such asymmetry is confined to this file.
 */
type BusinessResponseDto = Business;

function toBusiness(dto: BusinessResponseDto): Business {
	return {
		...dto,
		description: dto.description ?? null,
		addresses: dto.addresses ?? [],
		contacts: dto.contacts ?? [],
		services: dto.services ?? [],
	};
}

export const businessesRepository: Repository<Business, BusinessInput, BusinessInput> = {
	source: "live",

	async list(params?: ListParams) {
		const { data } = await apiClient.get<BusinessResponseDto[]>(endpoints.businesses.root, {
			signal: params?.signal,
		});
		return (data ?? []).map(toBusiness);
	},

	async get(id: Id, params?: ListParams) {
		const { data } = await apiClient.get<BusinessResponseDto>(endpoints.businesses.byId(id), {
			signal: params?.signal,
		});
		return toBusiness(data);
	},

	async create(input: BusinessInput) {
		const { data } = await apiClient.post<BusinessResponseDto>(
			endpoints.businesses.root,
			input,
		);
		return toBusiness(data);
	},

	async update(id: Id, input: BusinessInput) {
		await apiClient.put(endpoints.businesses.byId(id), input);
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
		const { data } = await apiClient.get<BusinessResponseDto>(endpoints.businesses.byId(id));
		await apiClient.delete(endpoints.businesses.byId(id), { data });
	},
};
