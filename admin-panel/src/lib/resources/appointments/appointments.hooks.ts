"use client";

import { useMutation, useQuery, useQueryClient, type QueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/query/queryKeys";
import type { Id } from "@/lib/api/types";
import {
	bookAppointment,
	changeAppointmentStatus,
	getAppointment,
	getAvailability,
	listAppointments,
	updateAppointment,
} from "./appointments.api";
import { salonOf, type SalonRef } from "./appointments.mappers";
import type { Appointment, AppointmentFilter, AppointmentInput, AppointmentStatus } from "./appointments.types";

/** businessId null means every salon, which only a SUPER_ADMIN can read. */
export function useAppointments(businessId: Id | null, filter: AppointmentFilter, enabled = true) {
	const query = useQuery({
		queryKey: queryKeys.appointments.list(businessId, filter),
		queryFn: ({ signal }) => listAppointments(businessId, filter, { signal }),
		enabled,
	});
	return { ...query, isPending: enabled && query.isPending, data: query.data ?? [] };
}

/** The /appointments/:id route carries no salon, so the caller works out which one it is in. */
export function useAppointment(businessId: Id | null, appointmentId: Id | null) {
	return useQuery({
		queryKey: queryKeys.appointments.detail(appointmentId ?? -1),
		queryFn: ({ signal }) => getAppointment(businessId as Id, appointmentId as Id, { signal }),
		enabled: businessId != null && appointmentId != null,
	});
}

/**
 * Slots change with every booking anywhere in the salon, and with its hours and shifts, so
 * they are fetched again whenever the form shows them rather than trusted for a minute.
 */
export function useAvailability(businessId: Id | null, serviceId: Id | null, date: string) {
	return useQuery({
		queryKey: queryKeys.appointments.availability(businessId ?? -1, serviceId ?? -1, date),
		queryFn: ({ signal }) => getAvailability(businessId as Id, serviceId as Id, date, { signal }),
		enabled: businessId != null && serviceId != null,
		staleTime: 0,
	});
}

/**
 * Every write answers with the appointment as stored, so its detail needs no refetch. Every list
 * it could be in, and every slot it took or freed, does.
 */
function stored(qc: QueryClient, appointment: Appointment) {
	qc.setQueryData(queryKeys.appointments.detail(appointment.id), appointment);
	qc.invalidateQueries({ queryKey: queryKeys.appointments.lists() });
	qc.invalidateQueries({ queryKey: queryKeys.appointments.availabilities() });
}

export function useBookAppointment() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: ({ salon, input }: { salon: SalonRef; input: AppointmentInput }) => bookAppointment(salon, input),
		onSuccess: (appointment) => stored(qc, appointment),
		// Refused mostly because someone took the time first: show what is still open.
		onError: () => qc.invalidateQueries({ queryKey: queryKeys.appointments.availabilities() }),
	});
}

export function useUpdateAppointment() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: ({ appointment, input }: { appointment: Appointment; input: AppointmentInput }) =>
			updateAppointment(salonOf(appointment), appointment.id, input),
		onSuccess: (appointment) => stored(qc, appointment),
		onError: (_error, { appointment }) => {
			qc.invalidateQueries({ queryKey: queryKeys.appointments.detail(appointment.id) });
			qc.invalidateQueries({ queryKey: queryKeys.appointments.availabilities() });
		},
	});
}

export function useChangeAppointmentStatus() {
	const qc = useQueryClient();
	return useMutation({
		mutationFn: ({ appointment, status }: { appointment: Appointment; status: AppointmentStatus }) =>
			changeAppointmentStatus(salonOf(appointment), appointment.id, status),
		onSuccess: (appointment) => stored(qc, appointment),
		// Refused mostly because someone else moved it on first: show where it is now.
		onError: (_error, { appointment }) =>
			qc.invalidateQueries({ queryKey: queryKeys.appointments.detail(appointment.id) }),
	});
}
