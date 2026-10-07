import type { Id } from "@/lib/api/types";
import type {
	Appointment,
	AppointmentFilter,
	AppointmentInput,
	AppointmentStatus,
	Availability,
	PersonRef,
} from "./appointments.types";

interface PersonRefDto {
	id: number;
	first_name: string;
	last_name: string;
}

export interface AppointmentDto {
	id: number;
	customer: PersonRefDto;
	staff: PersonRefDto;
	service: { id: number; name: string };
	starts_at: string;
	ends_at: string;
	status: AppointmentStatus;
	price: number;
	notes: string | null;
	created_at: string;
	updated_at: string | null;
}

interface AppointmentRequestDto {
	customer_id: number;
	staff_id: number;
	service_id: number;
	starts_at: string;
	notes: string | null;
}

export interface AvailabilityDto {
	timezone: string;
	duration_minutes: number;
	staff: (PersonRefDto & { slots: { starts_at: string; ends_at: string }[] })[];
}

/** What an appointment needs from its salon. The response carries none of it, so the caller supplies it. */
export interface SalonRef {
	id: Id;
	name: string;
	currency: string;
	timezone: string;
}

export function salonOf(appointment: Appointment): SalonRef {
	return {
		id: appointment.businessId,
		name: appointment.businessName,
		currency: appointment.currency,
		timezone: appointment.timezone,
	};
}

const toPersonRef = (dto: PersonRefDto): PersonRef => ({
	id: dto.id,
	firstName: dto.first_name,
	lastName: dto.last_name,
});

export function toAppointment(dto: AppointmentDto, salon: SalonRef): Appointment {
	return {
		id: dto.id,
		businessId: salon.id,
		businessName: salon.name,
		currency: salon.currency,
		timezone: salon.timezone,
		customer: toPersonRef(dto.customer),
		staff: toPersonRef(dto.staff),
		service: { id: dto.service.id, name: dto.service.name },
		startsAt: dto.starts_at,
		endsAt: dto.ends_at,
		status: dto.status,
		price: dto.price,
		notes: dto.notes,
		createdAt: dto.created_at,
		updatedAt: dto.updated_at,
	};
}

export function toAppointmentRequest(input: AppointmentInput): AppointmentRequestDto {
	const notes = input.notes?.trim() ?? "";
	return {
		customer_id: input.customerId,
		staff_id: input.staffId,
		service_id: input.serviceId,
		starts_at: input.startsAt,
		notes: notes === "" ? null : notes,
	};
}

/** The full-replace PUT body for an existing appointment, with some fields changed. */
export function appointmentInputFrom(
	appointment: Appointment,
	changes: Partial<AppointmentInput> = {},
): AppointmentInput {
	return {
		customerId: appointment.customer.id,
		staffId: appointment.staff.id,
		serviceId: appointment.service.id,
		startsAt: appointment.startsAt,
		notes: appointment.notes,
		...changes,
	};
}

/** Query parameters for the list. A filter left out is not sent, which means "any". */
export function toAppointmentQuery(filter: AppointmentFilter): Record<string, string | number> {
	const query: Record<string, string | number> = {};
	if (filter.from) query.from = filter.from;
	if (filter.to) query.to = filter.to;
	if (filter.staffId != null) query.staff_id = filter.staffId;
	if (filter.customerId != null) query.customer_id = filter.customerId;
	return query;
}

export function toAvailability(dto: AvailabilityDto): Availability {
	return {
		timezone: dto.timezone,
		durationMinutes: dto.duration_minutes,
		staff: dto.staff.map((member) => ({
			...toPersonRef(member),
			slots: member.slots.map((slot) => ({ startsAt: slot.starts_at, endsAt: slot.ends_at })),
		})),
	};
}
