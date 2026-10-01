import { apiClient } from "@/lib/api/client";
import { endpoints } from "@/lib/api/endpoints";
import type { Id, ListParams } from "@/lib/api/types";
import type { BusinessDto } from "@/lib/resources/businesses/businesses.mappers";
import type { UserDto } from "@/lib/resources/users/users.mappers";
import {
	toCreateStaffRequest,
	toEmployee,
	toUpdateStaffRequest,
	type StaffDto,
} from "./employees.mappers";
import type { CreateEmployeeInput, Employee, UpdateEmployeeInput } from "./employees.types";

/**
 * The staff endpoint only knows title/is_active/hired_at/calendar_colour/user_id - a name
 * and an email come from /users, joined in here by user_id. There is no "all businesses'
 * staff in one call" endpoint, so a platform-wide roster (businessId == null, i.e. a
 * SUPER_ADMIN) fans out one request per business plus one /users call, in parallel. That
 * mirrors the N+1 shape BE-15 already flags elsewhere in this API; there's no batching
 * endpoint to do better with yet.
 */
async function fetchEmployeesForBusinesses(
	businesses: Pick<BusinessDto, "id" | "name">[],
	params?: ListParams,
): Promise<Employee[]> {
	if (businesses.length === 0) return [];

	const [staffByBusiness, { data: users }] = await Promise.all([
		Promise.all(
			businesses.map(async (business) => {
				const { data } = await apiClient.get<StaffDto[]>(endpoints.businesses.staff(business.id), {
					signal: params?.signal,
				});
				return { business, staff: data };
			}),
		),
		apiClient.get<UserDto[]>(endpoints.users.root, { signal: params?.signal }),
	]);

	const usersById = new Map(users.map((u) => [u.id, u]));

	return staffByBusiness.flatMap(({ business, staff }) =>
		staff.flatMap((s) => {
			const user = usersById.get(s.user_id);
			// The FK guarantees this user exists; it only goes missing here if the caller
			// can see this business's staff but not this particular user (shouldn't happen
			// given StaffService's own business-match check) - drop it rather than show a
			// ghost row with a fabricated name.
			if (!user) return [];
			return [
				toEmployee(s, business.id, business.name, {
					firstName: user.first_name,
					lastName: user.last_name,
					email: user.email,
				}),
			];
		}),
	);
}

export async function listEmployees(businessId: Id | null, params?: ListParams): Promise<Employee[]> {
	const { data: allBusinesses } = await apiClient.get<BusinessDto[]>(endpoints.businesses.root, {
		signal: params?.signal,
	});
	const businesses =
		businessId == null ? allBusinesses : allBusinesses.filter((b) => b.id === businessId);
	return fetchEmployeesForBusinesses(businesses, params);
}

export async function createEmployee(businessId: Id, input: CreateEmployeeInput): Promise<Employee> {
	const { data } = await apiClient.post<StaffDto>(
		endpoints.businesses.staff(businessId),
		toCreateStaffRequest(input),
	);
	const { data: user } = await apiClient.get<UserDto>(endpoints.users.byId(input.userId));
	const { data: business } = await apiClient.get<BusinessDto>(endpoints.businesses.byId(businessId));
	return toEmployee(data, businessId, business.name, {
		firstName: user.first_name,
		lastName: user.last_name,
		email: user.email,
	});
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
