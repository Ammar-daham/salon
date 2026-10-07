"use client";

import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { resolveBusinessScope } from "@/lib/auth/scope";
import type { Id } from "@/lib/api/types";
import { getErrorMessage } from "@/lib/api/errors";
import { useBookAppointment } from "@/lib/resources/appointments/appointments.hooks";
import type { SalonRef } from "@/lib/resources/appointments/appointments.mappers";
import { personName, type AppointmentInput } from "@/lib/resources/appointments/appointments.types";
import { formatWallDate, timeOf } from "@/lib/utils/wallClock";
import PageHeader from "@/components/ui/PageHeader";
import EmptyState from "@/components/ui/EmptyState";
import { useToast } from "@/components/ui/toast/ToastProvider";
import { TaskIcon } from "@/icons";
import AppointmentForm from "./AppointmentForm";
import { appointmentHref } from "./appointmentLinks";

/**
 * Book an appointment. The ?business=, ?customer=, ?staff= and ?date= of a "Book" link say who and
 * when to start with; a salon's own staff always book at their own salon.
 */
export default function AppointmentCreateView({
	businessParam,
	customerParam,
	staffParam,
	dateParam,
}: {
	businessParam: Id | null;
	customerParam: Id | null;
	staffParam: Id | null;
	dateParam: string | null;
}) {
	const router = useRouter();
	const { user } = useAuth();
	const { toast } = useToast();
	const book = useBookAppointment();
	const scope = resolveBusinessScope(user);

	if (scope.kind === "unresolved") {
		return (
			<>
				<PageHeader title="Book appointment" />
				<EmptyState
					icon={<TaskIcon className="size-6" />}
					title="Your account isn't linked to a salon yet"
					description="Ask a platform administrator to attach your account to a business before booking."
				/>
			</>
		);
	}

	async function handleSubmit(input: AppointmentInput, salon: SalonRef) {
		try {
			const appointment = await book.mutateAsync({ salon, input });
			toast({
				tone: "success",
				title: "Appointment booked",
				description: `${appointment.service.name} for ${personName(appointment.customer)} with ${personName(appointment.staff)}, ${formatWallDate(appointment.startsAt)} at ${timeOf(appointment.startsAt)}.`,
			});
			router.push(appointmentHref(appointment));
		} catch (err) {
			toast({ tone: "error", title: "Couldn't book the appointment", description: getErrorMessage(err) });
		}
	}

	return (
		<>
			<PageHeader
				title="Book appointment"
				description="Pick a client and a service, then one of the open times."
			/>
			<div className="max-w-3xl">
				<AppointmentForm
					businessId={scope.kind === "business" ? scope.businessId : businessParam}
					pickBusiness={scope.kind === "platform"}
					prefill={{ customerId: customerParam, staffId: staffParam, date: dateParam }}
					submitLabel="Book appointment"
					submitting={book.isPending}
					onSubmit={handleSubmit}
					onCancel={() => router.back()}
				/>
			</div>
		</>
	);
}
