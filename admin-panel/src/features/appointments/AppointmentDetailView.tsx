"use client";

import { useState } from "react";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { can } from "@/lib/auth/permissions";
import { resolveBusinessScope } from "@/lib/auth/scope";
import type { Id } from "@/lib/api/types";
import { getErrorMessage, isApiError } from "@/lib/api/errors";
import { useAppointment, useChangeAppointmentStatus } from "@/lib/resources/appointments/appointments.hooks";
import { hasStarted, nextStatuses, waitsForStart } from "@/lib/resources/appointments/appointments.rules";
import {
	APPOINTMENT_STATUS_LABELS,
	APPOINTMENT_STATUS_TONE,
	personName,
	type Appointment,
	type AppointmentStatus,
} from "@/lib/resources/appointments/appointments.types";
import { formatMoney } from "@/lib/utils/money";
import { formatWallDate, timeOf } from "@/lib/utils/wallClock";

import PageHeader from "@/components/ui/PageHeader";
import EmptyState from "@/components/ui/EmptyState";
import Card from "@/components/ui/Card";
import StatusBadge from "@/components/ui/StatusBadge";
import Button from "@/components/ui/button/Button";
import ConfirmDialog from "@/components/ui/modal/ConfirmDialog";
import { Skeleton } from "@/components/ui/Skeleton";
import { useToast } from "@/components/ui/toast/ToastProvider";
import { ChevronLeftIcon, ErrorIcon, TaskIcon } from "@/icons";

const LONG_DATE: Intl.DateTimeFormatOptions = { weekday: "long", day: "numeric", month: "long", year: "numeric" };

const MOVE_LABELS: Partial<Record<AppointmentStatus, string>> = {
	CONFIRMED: "Confirm",
	COMPLETED: "Mark completed",
	NO_SHOW: "Mark no-show",
	CANCELLED: "Cancel appointment",
};

const MOVED_TOASTS: Partial<Record<AppointmentStatus, string>> = {
	CONFIRMED: "Appointment confirmed",
	COMPLETED: "Marked completed",
	NO_SHOW: "Marked as a no-show",
	CANCELLED: "Appointment cancelled",
};

function formatInstant(iso: string) {
	return new Date(iso).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
}

/** What a final move says before it is made: none of them can be undone. */
function finalMoveDialog(appointment: Appointment, status: AppointmentStatus) {
	const customer = personName(appointment.customer);
	const staff = personName(appointment.staff);
	const service = appointment.service.name;
	switch (status) {
		case "COMPLETED":
			return {
				title: "Mark as completed?",
				description: `${customer}'s ${service} with ${staff} took place. This is final: a completed appointment can't be changed again.`,
			};
		case "NO_SHOW":
			return {
				title: "Mark as a no-show?",
				description: `${customer} didn't come to their ${service} with ${staff}. This is final, and it stays on their record.`,
			};
		default:
			return {
				title: "Cancel this appointment?",
				description: `This frees ${staff}'s time on ${formatWallDate(appointment.startsAt)} at ${timeOf(appointment.startsAt)}. It stays in ${customer}'s history as cancelled and can't be reopened, so book a new one if plans change again.`,
			};
	}
}

/**
 * One appointment, and the moves its status can make. `businessParam` is the ?business= of the
 * link, which only a SUPER_ADMIN needs: anyone else reads their own salon's appointments.
 */
export default function AppointmentDetailView({ id, businessParam }: { id: Id; businessParam: Id | null }) {
	const { user } = useAuth();
	const { toast } = useToast();
	const scope = resolveBusinessScope(user);
	const businessId =
		scope.kind === "business" ? scope.businessId : scope.kind === "platform" ? businessParam : null;

	const { data: appointment, isPending, isError, error, refetch } = useAppointment(businessId, id);
	const change = useChangeAppointmentStatus();
	// A final move waiting for the dialog's answer.
	const [confirming, setConfirming] = useState<AppointmentStatus | null>(null);

	const backLink = (
		<Link
			href="/appointments"
			className="mb-4 inline-flex items-center gap-1 text-sm text-ink-muted transition-colors hover:text-ink"
		>
			<ChevronLeftIcon className="size-4" />
			All appointments
		</Link>
	);

	if (businessId == null || (isError && isApiError(error) && error.kind === "not-found")) {
		return (
			<>
				{backLink}
				<EmptyState
					icon={<TaskIcon className="size-6" />}
					title="Appointment not found"
					description="It may belong to another salon. Open it from the appointments list."
				/>
			</>
		);
	}

	if (isError) {
		return (
			<>
				{backLink}
				<EmptyState
					icon={<ErrorIcon className="size-6" />}
					title="Couldn't load this appointment"
					description={getErrorMessage(error)}
					action={<Button variant="outline" onClick={() => refetch()}>Try again</Button>}
				/>
			</>
		);
	}

	if (isPending) {
		return (
			<div className="flex flex-col gap-4">
				<Skeleton className="h-6 w-40" />
				<Skeleton className="h-16 w-full max-w-md" />
				<Skeleton className="h-64 rounded-card" />
			</div>
		);
	}

	const started = hasStarted(appointment);
	const moves = nextStatuses(appointment.status).filter((status) =>
		can(user, status === "CANCELLED" ? "appointment:cancel" : "appointment:edit"),
	);

	async function move(status: AppointmentStatus) {
		if (!appointment) return;
		try {
			await change.mutateAsync({ appointment, status });
			toast({ tone: "success", title: MOVED_TOASTS[status] ?? "Appointment updated" });
		} catch (err) {
			toast({ tone: "error", title: "Couldn't update the appointment", description: getErrorMessage(err) });
		} finally {
			setConfirming(null);
		}
	}

	const dialog = confirming ? finalMoveDialog(appointment, confirming) : null;

	return (
		<>
			{backLink}

			<PageHeader
				title={appointment.service.name}
				description={`${formatWallDate(appointment.startsAt, LONG_DATE)}, ${timeOf(appointment.startsAt)}–${timeOf(appointment.endsAt)}`}
			/>

			<div className="mb-6 flex flex-wrap items-center gap-3">
				<StatusBadge tone={APPOINTMENT_STATUS_TONE[appointment.status]}>
					{APPOINTMENT_STATUS_LABELS[appointment.status]}
				</StatusBadge>
				<span className="text-sm text-ink-subtle">
					On {appointment.businessName}&apos;s clock, {appointment.timezone.replaceAll("_", " ")}
				</span>
			</div>

			<div className="grid grid-cols-1 gap-4 md:gap-6 lg:grid-cols-3">
				<div className="flex flex-col gap-4 md:gap-6 lg:col-span-2">
					<Card title="Booking">
						<dl className="flex flex-col gap-3 text-sm">
							<div className="flex justify-between gap-4">
								<dt className="text-ink-muted">Client</dt>
								<dd>
									{can(user, "customer:list") ? (
										<Link
											href={`/customers/${appointment.customer.id}`}
											className="text-ink hover:text-primary-700 dark:hover:text-primary-300"
										>
											{personName(appointment.customer)}
										</Link>
									) : (
										<span className="text-ink">{personName(appointment.customer)}</span>
									)}
								</dd>
							</div>
							<div className="flex justify-between gap-4">
								<dt className="text-ink-muted">With</dt>
								<dd>
									{can(user, "employee:list") ? (
										<Link
											href={`/employees/${appointment.staff.id}`}
											className="text-ink hover:text-primary-700 dark:hover:text-primary-300"
										>
											{personName(appointment.staff)}
										</Link>
									) : (
										<span className="text-ink">{personName(appointment.staff)}</span>
									)}
								</dd>
							</div>
							<div className="flex justify-between gap-4">
								<dt className="text-ink-muted">Service</dt>
								<dd className="text-ink">{appointment.service.name}</dd>
							</div>
							<div className="flex justify-between gap-4">
								<dt className="text-ink-muted">Price</dt>
								<dd className="tabular-nums text-ink">{formatMoney(appointment.price, appointment.currency)}</dd>
							</div>
							{scope.kind === "platform" && (
								<div className="flex justify-between gap-4">
									<dt className="text-ink-muted">Salon</dt>
									<dd>
										<Link
											href={`/businesses/${appointment.businessId}`}
											className="text-ink hover:text-primary-700 dark:hover:text-primary-300"
										>
											{appointment.businessName}
										</Link>
									</dd>
								</div>
							)}
							<div className="flex justify-between gap-4">
								<dt className="text-ink-muted">Booked</dt>
								<dd className="text-ink">{formatInstant(appointment.createdAt)}</dd>
							</div>
							{appointment.updatedAt && (
								<div className="flex justify-between gap-4">
									<dt className="text-ink-muted">Last changed</dt>
									<dd className="text-ink">{formatInstant(appointment.updatedAt)}</dd>
								</div>
							)}
						</dl>
					</Card>

					<Card title="Notes">
						{appointment.notes ? (
							<p className="whitespace-pre-line text-sm text-ink">{appointment.notes}</p>
						) : (
							<p className="text-sm text-ink-subtle">No notes for this appointment.</p>
						)}
					</Card>
				</div>

				<Card title="Status" className="self-start">
					{moves.length === 0 ? (
						<p className="text-sm text-ink-muted">
							{nextStatuses(appointment.status).length === 0
								? `This appointment is ${APPOINTMENT_STATUS_LABELS[appointment.status].toLowerCase()}. That is final, and it stays in ${personName(appointment.customer)}'s history.`
								: "You can see this appointment but not change it."}
						</p>
					) : (
						<div className="flex flex-col gap-3">
							<p className="text-sm text-ink-muted">
								{appointment.status === "BOOKED"
									? "Confirm it once the client has agreed to the time, or move it straight on once it has happened."
									: "Confirmed with the client. Mark how it went once it has started."}
							</p>
							{moves.map((status) => {
								const tooEarly = waitsForStart(status) && !started;
								return (
									<Button
										key={status}
										variant={status === "CONFIRMED" ? "primary" : "outline"}
										disabled={tooEarly || change.isPending}
										loading={change.isPending && change.variables?.status === status}
										onClick={() => (status === "CONFIRMED" ? move(status) : setConfirming(status))}
										className={
											status === "CANCELLED"
												? "text-error-600 hover:bg-error-50 dark:text-error-300 dark:hover:bg-error-500/10"
												: undefined
										}
									>
										{MOVE_LABELS[status]}
									</Button>
								);
							})}
							{!started && moves.some(waitsForStart) && (
								<p className="text-sm text-ink-subtle">
									It can be marked completed or a no-show once it has started, at{" "}
									{timeOf(appointment.startsAt)} on {formatWallDate(appointment.startsAt)}.
								</p>
							)}
						</div>
					)}
				</Card>
			</div>

			<ConfirmDialog
				isOpen={confirming !== null}
				onClose={() => setConfirming(null)}
				onConfirm={() => (confirming ? move(confirming) : undefined)}
				loading={change.isPending}
				destructive={false}
				title={dialog?.title ?? ""}
				description={dialog?.description ?? null}
				confirmLabel={confirming ? MOVE_LABELS[confirming] : undefined}
				cancelLabel={confirming === "CANCELLED" ? "Keep it" : "Not yet"}
			/>
		</>
	);
}
