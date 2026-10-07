import { apiClient } from "@/lib/api/client";
import { endpoints } from "@/lib/api/endpoints";
import type { Id, ListParams } from "@/lib/api/types";
import type { BusinessDto } from "@/lib/resources/businesses/businesses.mappers";
import {
	toAppointment,
	toAppointmentQuery,
	toAppointmentRequest,
	toAvailability,
	type AppointmentDto,
	type AvailabilityDto,
	type SalonRef,
} from "./appointments.mappers";
import type {
	Appointment,
	AppointmentFilter,
	AppointmentInput,
	AppointmentStatus,
	Availability,
} from "./appointments.types";

const toSalonRef = (dto: BusinessDto): SalonRef => ({
	id: dto.id,
	name: dto.name,
	currency: dto.currency,
	timezone: dto.timezone,
});

/**
 * Only a salon's own staff (and a SUPER_ADMIN) can read its appointments, so a salon-scoped
 * caller reads one salon. A platform caller (businessId == null) fans out one request per salon,
 * in parallel, as customers do: there is no cross-salon endpoint. Each salon's come in start order.
 */
export async function listAppointments(
	businessId: Id | null,
	filter: AppointmentFilter,
	params?: ListParams,
): Promise<Appointment[]> {
	const signal = params?.signal;
	const salons =
		businessId == null
			? (await apiClient.get<BusinessDto[]>(endpoints.businesses.root, { signal })).data.map(toSalonRef)
			: [toSalonRef((await apiClient.get<BusinessDto>(endpoints.businesses.byId(businessId), { signal })).data)];

	const perSalon = await Promise.all(
		salons.map(async (salon) => {
			const { data } = await apiClient.get<AppointmentDto[]>(endpoints.businesses.appointments(salon.id), {
				params: toAppointmentQuery(filter),
				signal,
			});
			return data.map((dto) => toAppointment(dto, salon));
		}),
	);
	return perSalon.flat();
}

export async function getAppointment(businessId: Id, appointmentId: Id, params?: ListParams): Promise<Appointment> {
	const [{ data }, { data: business }] = await Promise.all([
		apiClient.get<AppointmentDto>(endpoints.businesses.appointmentById(businessId, appointmentId), {
			signal: params?.signal,
		}),
		apiClient.get<BusinessDto>(endpoints.businesses.byId(businessId), { signal: params?.signal }),
	]);
	return toAppointment(data, toSalonRef(business));
}

/** The response is the appointment as stored, with the end and price the server worked out. */
export async function bookAppointment(salon: SalonRef, input: AppointmentInput): Promise<Appointment> {
	const { data } = await apiClient.post<AppointmentDto>(
		endpoints.businesses.appointments(salon.id),
		toAppointmentRequest(input),
	);
	return toAppointment(data, salon);
}

/** Replaces who, what and when. Only a booked or confirmed appointment can be changed. */
export async function updateAppointment(
	salon: SalonRef,
	appointmentId: Id,
	input: AppointmentInput,
): Promise<Appointment> {
	const { data } = await apiClient.put<AppointmentDto>(
		endpoints.businesses.appointmentById(salon.id, appointmentId),
		toAppointmentRequest(input),
	);
	return toAppointment(data, salon);
}

/** Confirms, completes, cancels or marks a no-show. There is no DELETE: cancelling is how one goes. */
export async function changeAppointmentStatus(
	salon: SalonRef,
	appointmentId: Id,
	status: AppointmentStatus,
): Promise<Appointment> {
	const { data } = await apiClient.put<AppointmentDto>(
		endpoints.businesses.appointmentStatus(salon.id, appointmentId),
		{ status },
	);
	return toAppointment(data, salon);
}

/** One day's open slots for a service, per staff member who performs it. Past times are left out. */
export async function getAvailability(
	businessId: Id,
	serviceId: Id,
	date: string,
	params?: ListParams,
): Promise<Availability> {
	const { data } = await apiClient.get<AvailabilityDto>(endpoints.businesses.availability(businessId, serviceId), {
		params: { from: date, to: date },
		signal: params?.signal,
	});
	return toAvailability(data);
}
