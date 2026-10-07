"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { resolveBusinessScope } from "@/lib/auth/scope";
import type { Id } from "@/lib/api/types";
import { getErrorMessage } from "@/lib/api/errors";
import { useAppointment, useUpdateAppointment } from "@/lib/resources/appointments/appointments.hooks";
import { isOpen } from "@/lib/resources/appointments/appointments.rules";
import {
	APPOINTMENT_STATUS_LABELS,
	type AppointmentInput,
} from "@/lib/resources/appointments/appointments.types";
import PageHeader from "@/components/ui/PageHeader";
import EmptyState from "@/components/ui/EmptyState";
import Button from "@/components/ui/button/Button";
import { Skeleton } from "@/components/ui/Skeleton";
import { useToast } from "@/components/ui/toast/ToastProvider";
import { ChevronLeftIcon, ErrorIcon, TaskIcon } from "@/icons";
import AppointmentForm from "./AppointmentForm";
import { appointmentHref } from "./appointmentLinks";

/** Move an appointment, or change its client, service or notes. Only a booked or confirmed one can be. */
export default function AppointmentEditView({ id, businessParam }: { id: Id; businessParam: Id | null }) {
	const router = useRouter();
	const { user } = useAuth();
	const { toast } = useToast();
	const scope = resolveBusinessScope(user);
	const businessId =
		scope.kind === "business" ? scope.businessId : scope.kind === "platform" ? businessParam : null;

	const { data: appointment, isPending, isError, error, refetch } = useAppointment(businessId, id);
	const update = useUpdateAppointment();

	if (businessId == null) {
		return (
			<EmptyState
				icon={<TaskIcon className="size-6" />}
				title="Appointment not found"
				description="It may belong to another salon. Open it from the appointments list."
			/>
		);
	}

	if (isError) {
		return (
			<EmptyState
				icon={<ErrorIcon className="size-6" />}
				title="Couldn't load this appointment"
				description={getErrorMessage(error)}
				action={<Button variant="outline" onClick={() => refetch()}>Try again</Button>}
			/>
		);
	}

	if (isPending) {
		return <Skeleton className="h-96 max-w-3xl rounded-card" />;
	}

	const backLink = (
		<Link
			href={appointmentHref(appointment)}
			className="mb-4 inline-flex items-center gap-1 text-sm text-ink-muted transition-colors hover:text-ink"
		>
			<ChevronLeftIcon className="size-4" />
			Back to the appointment
		</Link>
	);

	if (!isOpen(appointment.status)) {
		return (
			<>
				{backLink}
				<EmptyState
					icon={<TaskIcon className="size-6" />}
					title="This appointment can't be changed"
					description={`It is ${APPOINTMENT_STATUS_LABELS[appointment.status].toLowerCase()}, which is final. Book a new one instead.`}
				/>
			</>
		);
	}

	async function handleSubmit(input: AppointmentInput) {
		if (!appointment) return;
		try {
			const saved = await update.mutateAsync({ appointment, input });
			toast({ tone: "success", title: "Appointment updated" });
			router.push(appointmentHref(saved));
		} catch (err) {
			toast({ tone: "error", title: "Couldn't update the appointment", description: getErrorMessage(err) });
		}
	}

	return (
		<>
			{backLink}
			<PageHeader
				title="Edit appointment"
				description="Move it to another time or staff member, or change the client, service or notes."
			/>
			<div className="max-w-3xl">
				<AppointmentForm
					// Starts again from what is stored if someone else changes it while this is open.
					key={appointment.updatedAt ?? appointment.createdAt}
					businessId={appointment.businessId}
					initial={appointment}
					submitLabel="Save changes"
					submitting={update.isPending}
					onSubmit={handleSubmit}
					onCancel={() => router.push(appointmentHref(appointment))}
				/>
			</div>
		</>
	);
}
