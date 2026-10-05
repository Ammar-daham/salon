import { apiClient } from "@/lib/api/client";
import { endpoints } from "@/lib/api/endpoints";
import type { Id, ListParams } from "@/lib/api/types";
import type { BusinessDto } from "@/lib/resources/businesses/businesses.mappers";
import { toCustomer, toCustomerRequest, type CustomerDto } from "./customers.mappers";
import type { Customer, CustomerInput } from "./customers.types";

/**
 * Customers are business-scoped and the backend 403s any other salon's list, so a
 * salon-scoped caller fetches exactly one list. Only a platform caller (businessId
 * == null, i.e. SUPER_ADMIN) fans out one request per business, in parallel - the
 * same N+1 shape as employees, for the same reason: there is no cross-salon endpoint.
 */
export async function listCustomers(businessId: Id | null, params?: ListParams): Promise<Customer[]> {
	const { data: allBusinesses } = await apiClient.get<BusinessDto[]>(endpoints.businesses.root, {
		signal: params?.signal,
	});
	const businesses =
		businessId == null ? allBusinesses : allBusinesses.filter((b) => b.id === businessId);

	const perBusiness = await Promise.all(
		businesses.map(async (business) => {
			const { data } = await apiClient.get<CustomerDto[]>(endpoints.businesses.customers(business.id), {
				signal: params?.signal,
			});
			return data.map((dto) => toCustomer(dto, business.id, business.name));
		}),
	);
	return perBusiness.flat();
}

export async function createCustomer(businessId: Id, input: CustomerInput): Promise<Customer> {
	const [{ data }, { data: business }] = await Promise.all([
		apiClient.post<CustomerDto>(endpoints.businesses.customers(businessId), toCustomerRequest(input)),
		apiClient.get<BusinessDto>(endpoints.businesses.byId(businessId)),
	]);
	return toCustomer(data, businessId, business.name);
}

/** A full replace: every field in `input` is written, and a null clears it. */
export async function updateCustomer(businessId: Id, customerId: Id, input: CustomerInput): Promise<void> {
	await apiClient.put(endpoints.businesses.customerById(businessId, customerId), toCustomerRequest(input));
}

export async function removeCustomer(businessId: Id, customerId: Id): Promise<void> {
	await apiClient.delete(endpoints.businesses.customerById(businessId, customerId));
}
