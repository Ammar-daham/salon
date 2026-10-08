import { apiClient } from "@/lib/api/client";
import { endpoints } from "@/lib/api/endpoints";
import { pageParams, toPage, type PageDto } from "@/lib/api/paging";
import type { Id, ListParams, Page, PageQuery } from "@/lib/api/types";
import { toCustomer, toCustomerRequest, type CustomerDto } from "./customers.mappers";
import type { Customer, CustomerInput } from "./customers.types";

/**
 * sort is name (last name first), created_at or business_name; q matches the name, email or phone.
 * businessId narrows to one salon, or null for every salon the caller may see.
 */
export interface CustomerPageQuery extends PageQuery {
	businessId?: Id | null;
}

/**
 * Customers across salons (BE-15). The server decides whose: every salon's for a SUPER_ADMIN, only
 * their own salon's for anyone else, and naming another salon is a 403.
 */
export async function listCustomers(query: CustomerPageQuery, params?: ListParams): Promise<Page<Customer>> {
	const { data } = await apiClient.get<PageDto<CustomerDto>>(endpoints.customers.root, {
		params: pageParams(query, { business_id: query.businessId }),
		signal: params?.signal,
	});
	return toPage(data, toCustomer);
}

/** By id alone, as the /customers/:id pages have no salon in their path. Another salon's is a 404. */
export async function getCustomer(customerId: Id, params?: ListParams): Promise<Customer> {
	const { data } = await apiClient.get<CustomerDto>(endpoints.customers.byId(customerId), {
		signal: params?.signal,
	});
	return toCustomer(data);
}

export async function createCustomer(businessId: Id, input: CustomerInput): Promise<Customer> {
	const { data } = await apiClient.post<CustomerDto>(
		endpoints.businesses.customers(businessId),
		toCustomerRequest(input),
	);
	return toCustomer(data);
}

/** A full replace: every field in `input` is written, and a null clears it. */
export async function updateCustomer(businessId: Id, customerId: Id, input: CustomerInput): Promise<void> {
	await apiClient.put(endpoints.businesses.customerById(businessId, customerId), toCustomerRequest(input));
}

export async function removeCustomer(businessId: Id, customerId: Id): Promise<void> {
	await apiClient.delete(endpoints.businesses.customerById(businessId, customerId));
}
