import { apiClient } from "@/lib/api/client";
import { endpoints } from "@/lib/api/endpoints";
import type { Id, ListParams } from "@/lib/api/types";
import type { BusinessDto } from "@/lib/resources/businesses/businesses.mappers";
import { listAllBusinesses } from "@/lib/resources/businesses/businesses.api";
import {
	toCreateStaffRequest,
	toEmployee,
	toUpdateStaffRequest,
	type StaffDto,
} from "./employees.mappers";
import type { CreateEmployeeInput, Employee, UpdateEmployeeInput } from "./employees.types";

/**
 * Each salon's staff, named by the staff records themselves (BE-15). There is no "every salon's staff
 * in one call" endpoint, so a platform-wide roster (businessId == null, i.e. a SUPER_ADMIN) reads
 * every salon's, in parallel; a salon's roster is short, and the platform's salons are few.
 */
async function fetchEmployeesForBusinesses(
	businesses: { id: Id; name: string }[],
	params?: ListParams,
): Promise<Employee[]> {
	const staffByBusiness = await Promise.all(
		businesses.map(async (business) => {
			const { data } = await apiClient.get<StaffDto[]>(endpoints.businesses.staff(business.id), {
				signal: params?.signal,
			});
			return data.map((s) => toEmployee(s, business.id, business.name));
		}),
	);
	return staffByBusiness.flat();
}

export async function listEmployees(businessId: Id | null, params?: ListParams): Promise<Employee[]> {
	if (businessId == null) {
		return fetchEmployeesForBusinesses(await listAllBusinesses(params), params);
	}
	const { data: business } = await apiClient.get<BusinessDto>(endpoints.businesses.byId(businessId), {
		signal: params?.signal,
	});
	return fetchEmployeesForBusinesses([business], params);
}

export async function createEmployee(businessId: Id, input: CreateEmployeeInput): Promise<Employee> {
	const [{ data }, { data: business }] = await Promise.all([
		apiClient.post<StaffDto>(endpoints.businesses.staff(businessId), toCreateStaffRequest(input)),
		apiClient.get<BusinessDto>(endpoints.businesses.byId(businessId)),
	]);
	return toEmployee(data, businessId, business.name);
}

export async function updateEmployee(
	businessId: Id,
	staffId: Id,
	input: UpdateEmployeeInput,
): Promise<void> {
	await apiClient.put(endpoints.businesses.staffById(businessId, staffId), toUpdateStaffRequest(input));
}

/**
 * Removes the employment record only - the backend deliberately never deletes the
 * underlying user account here. Firing someone isn't the same as deleting their login.
 */
export async function removeEmployee(businessId: Id, staffId: Id): Promise<void> {
	await apiClient.delete(endpoints.businesses.staffById(businessId, staffId));
}
