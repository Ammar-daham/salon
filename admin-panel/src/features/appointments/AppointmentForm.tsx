"use client";

import { useMemo, useState, type FormEvent } from "react";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { can } from "@/lib/auth/permissions";
import type { Id } from "@/lib/api/types";
import { getErrorMessage } from "@/lib/api/errors";
import { useBusiness, useBusinesses } from "@/lib/resources/businesses/businesses.hooks";
import { useCustomers } from "@/lib/resources/customers/customers.hooks";
import { useAvailability } from "@/lib/resources/appointments/appointments.hooks";
import type { SalonRef } from "@/lib/resources/appointments/appointments.mappers";
import {
	personName,
	type Appointment,
	type AppointmentInput,
	type PersonRef,
} from "@/lib/resources/appointments/appointments.types";
import { formatMoney } from "@/lib/utils/money";
import { addDays, formatWallDate, plusMinutes, timeOf, todayOn } from "@/lib/utils/wallClock";
import { cn } from "@/lib/utils/cn";

import Card from "@/components/ui/Card";
import Button from "@/components/ui/button/Button";
import Field, { SelectInput, TextInput, TextareaInput } from "@/components/ui/form/Field";
import { Skeleton } from "@/components/ui/Skeleton";
import { ChevronLeftIcon } from "@/icons";

type Errors = Partial<Record<"business" | "customer" | "service" | "time", string>>;

// Mirrors the backend's AppointmentRequest limit.
const NOTES_MAX = 5000;

/** A time to book: picked from the open slots, or typed in for one staff member. */
interface Choice {
	staffId: Id;
	startsAt: string;
}

interface AppointmentFormProps {
	/** The salon to book at; for a SUPER_ADMIN, the one the salon picker starts on, if any. */
	businessId: Id | null;
	/** A SUPER_ADMIN picks the salon in the form; anyone else books at their own. */
	pickBusiness?: boolean;
	/** The appointment being changed. Leave out to book a new one. */
	initial?: Appointment;
	/** Who and when to start with when booking from a client's or staff member's page, or the calendar. */
	prefill?: { customerId?: Id | null; staffId?: Id | null; date?: string | null };
	submitLabel: string;
	submitting: boolean;
	onSubmit: (input: AppointmentInput, salon: SalonRef) => void | Promise<void>;
	onCancel: () => void;
}

/**
 * Book or change an appointment from the salon's open slots. Slots come from availability, which
 * is advice: the backend only refuses a double booking. So a time can also be typed in for one
 * staff member, to fit someone in after hours, and changing an appointment can keep its own time,
 * which availability counts as taken.
 *
 * Changing one replaces every field, and only what changes is checked: a client, service or staff
 * member removed since stays on offer as it is.
 */
export default function AppointmentForm({
	businessId,
	pickBusiness = false,
	initial,
	prefill,
	submitLabel,
	submitting,
	onSubmit,
	onCancel,
}: AppointmentFormProps) {
	const { user } = useAuth();

	const [pickedBusiness, setPickedBusiness] = useState(businessId != null ? String(businessId) : "");
	const salonId = pickBusiness ? (pickedBusiness ? Number(pickedBusiness) : null) : businessId;

	const { data: businesses, isPending: businessesPending } = useBusinesses();
	const { data: business, isPending: businessPending } = useBusiness(salonId);
	const { data: customers, isPending: customersPending } = useCustomers(
		salonId != null ? { kind: "business", businessId: salonId } : { kind: "unresolved" },
	);

	const [customerId, setCustomerId] = useState(String(initial?.customer.id ?? prefill?.customerId ?? ""));
	const [serviceId, setServiceId] = useState(String(initial?.service.id ?? ""));
	// null until moved: today on the salon's clock, or the day of the appointment being changed.
	const [date, setDate] = useState<string | null>(initial ? initial.startsAt.slice(0, 10) : (prefill?.date ?? null));
	const [staffFilter, setStaffFilter] = useState(String(initial?.staff.id ?? prefill?.staffId ?? ""));
	const [picked, setPicked] = useState<Choice | null>(
		initial ? { staffId: initial.staff.id, startsAt: initial.startsAt } : null,
	);
	const [customTime, setCustomTime] = useState("");
	const [notes, setNotes] = useState(initial?.notes ?? "");
	const [errors, setErrors] = useState<Errors>({});

	const day = date ?? (business ? todayOn(business.timezone) : null);
	const service = business?.services.find((s) => String(s.id) === serviceId) ?? null;
	const availability = useAvailability(salonId, serviceId ? Number(serviceId) : null, day);

	// What is on offer, plus whatever the appointment being changed already has, even if it is gone.
	const customerOptions = useMemo(() => {
		const options = [...customers]
			.sort((a, b) => a.lastName.localeCompare(b.lastName) || a.firstName.localeCompare(b.firstName))
			.map((c) => ({
				id: c.id,
				label: `${c.firstName} ${c.lastName}${c.phone ? ` · ${c.phone}` : c.email ? ` · ${c.email}` : ""}`,
			}));
		if (initial && !customersPending && !options.some((c) => c.id === initial.customer.id)) {
			options.unshift({ id: initial.customer.id, label: `${personName(initial.customer)} (removed)` });
		}
		return options;
	}, [customers, customersPending, initial]);

	const serviceOptions = useMemo(() => {
		if (!business) return [];
		const options = business.services
			.filter((s) => s.isActive || s.id === initial?.service.id)
			.map((s) => ({
				id: s.id,
				label: `${s.name} · ${s.durationMinutes} min · ${formatMoney(s.price, business.currency)}${s.isActive ? "" : " (inactive)"}`,
			}));
		if (initial && !options.some((s) => s.id === initial.service.id)) {
			options.unshift({ id: initial.service.id, label: `${initial.service.name} (removed)` });
		}
		return options;
	}, [business, initial]);

	const performers = useMemo(() => availability.data?.staff ?? [], [availability.data]);
	const staffOptions = useMemo(() => {
		const options: PersonRef[] = [...performers];
		if (initial && !options.some((s) => s.id === initial.staff.id)) options.push(initial.staff);
		return options;
	}, [performers, initial]);
	const staffName = (id: Id) => {
		const person = staffOptions.find((s) => s.id === id);
		return person ? personName(person) : "this staff member";
	};

	// A choice that isn't on offer (yet) reads as none, so a select never shows one thing and sends another.
	const customer = customerOptions.some((c) => String(c.id) === customerId) ? customerId : "";
	const staff = staffOptions.some((s) => String(s.id) === staffFilter) ? staffFilter : "";
	const staffRows = staff ? performers.filter((s) => String(s.id) === staff) : performers;

	// A typed-in time wins over a picked slot, for whoever the staff filter names.
	const custom = customTime && staff && day ? { staffId: Number(staff), startsAt: `${day}T${customTime}` } : null;
	const choice: Choice | null = custom ?? picked;
	// The backend keeps an appointment's length unless its service changes, even if the service's has since.
	const duration =
		initial && serviceId === String(initial.service.id)
			? minutesBetween(initial.startsAt, initial.endsAt)
			: (service?.durationMinutes ?? availability.data?.durationMinutes ?? null);
	const isInitialTime =
		!!initial && choice?.staffId === initial.staff.id && choice.startsAt === initial.startsAt;

	function chooseSalon(value: string) {
		setPickedBusiness(value);
		setCustomerId("");
		setServiceId("");
		setDate(null);
		setStaffFilter("");
		setPicked(null);
		setCustomTime("");
	}

	function chooseService(value: string) {
		setServiceId(value);
		// Slots are per service, and a picked one may not fit another's length.
		setPicked(null);
		setCustomTime("");
	}

	function chooseStaff(value: string) {
		setStaffFilter(value);
		setCustomTime("");
	}

	function validate(): Errors {
		const found: Errors = {};
		if (salonId == null) found.business = "Choose which salon to book at.";
		if (!customer) found.customer = "Choose a client.";
		if (!serviceOptions.some((s) => String(s.id) === serviceId)) found.service = "Choose a service.";
		if (!choice) found.time = "Pick an open time, or enter another one for a staff member.";
		return found;
	}

	async function handleSubmit(e: FormEvent<HTMLFormElement>) {
		e.preventDefault();
		const found = validate();
		setErrors(found);
		if (Object.keys(found).length > 0 || !business || !choice) return;

		await onSubmit(
			{
				customerId: Number(customer),
				staffId: choice.staffId,
				serviceId: Number(serviceId),
				startsAt: choice.startsAt,
				notes,
			},
			{ id: business.id, name: business.name, currency: business.currency, timezone: business.timezone },
		);
	}

	return (
		<form onSubmit={handleSubmit} className="flex flex-col gap-4 md:gap-6">
			<Card title="Client and service">
				<div className="flex flex-col gap-5">
					{pickBusiness && (
						<Field label="Salon" required error={errors.business}>
							{(p) => (
								<SelectInput
									{...p}
									value={pickedBusiness}
									onChange={(e) => chooseSalon(e.target.value)}
									disabled={businessesPending}
								>
									<option value="">{businessesPending ? "Loading salons…" : "Select a salon…"}</option>
									{(businesses ?? []).map((b) => (
										<option key={b.id} value={b.id}>
											{b.name}
										</option>
									))}
								</SelectInput>
							)}
						</Field>
					)}

					<div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
						<Field
							label="Client"
							required
							error={errors.customer}
							hint={salonId != null && !customersPending && customers.length === 0 ? "This salon has no clients yet." : undefined}
						>
							{(p) => (
								<SelectInput
									{...p}
									value={customer}
									onChange={(e) => setCustomerId(e.target.value)}
									disabled={salonId == null || customersPending}
								>
									<option value="">{salonId != null && customersPending ? "Loading clients…" : "Select a client…"}</option>
									{customerOptions.map((c) => (
										<option key={c.id} value={c.id}>
											{c.label}
										</option>
									))}
								</SelectInput>
							)}
						</Field>

						<Field label="Service" required error={errors.service}>
							{(p) => (
								<SelectInput
									{...p}
									value={serviceId}
									onChange={(e) => chooseService(e.target.value)}
									disabled={salonId == null || businessPending}
								>
									<option value="">{salonId != null && businessPending ? "Loading services…" : "Select a service…"}</option>
									{serviceOptions.map((s) => (
										<option key={s.id} value={s.id}>
											{s.label}
										</option>
									))}
								</SelectInput>
							)}
						</Field>
					</div>

					{can(user, "customer:create") && (
						<p className="text-sm text-ink-muted">
							New here?{" "}
							<Link href="/customers/new" className="font-medium text-primary-700 hover:underline dark:text-primary-300">
								Add a client
							</Link>{" "}
							first, then come back to book.
						</p>
					)}
				</div>
			</Card>

			<Card
				title="Time"
				description={
					business
						? `Open times are on ${business.name}'s clock, ${business.timezone.replaceAll("_", " ")}: inside its opening hours and the staff member's shift, clear of their time off and other bookings.`
						: undefined
				}
			>
				<div className="flex flex-col gap-5">
					<div className="flex flex-col gap-5 sm:flex-row sm:items-end">
						<Field label="Day">
							{(p) => (
								<div className="flex items-center gap-2">
									<Button
										size="sm"
										variant="ghost"
										aria-label="Previous day"
										disabled={!day}
										onClick={() => day && setDate(addDays(day, -1))}
										startIcon={<ChevronLeftIcon className="size-4" />}
									/>
									<TextInput
										{...p}
										type="date"
										value={day ?? ""}
										onChange={(e) => e.target.value && setDate(e.target.value)}
										disabled={!day}
										className="w-44"
									/>
									<Button
										size="sm"
										variant="ghost"
										aria-label="Next day"
										disabled={!day}
										onClick={() => day && setDate(addDays(day, 1))}
										startIcon={<ChevronLeftIcon className="size-4 rotate-180" />}
									/>
								</div>
							)}
						</Field>

						<Field label="Staff member" className="sm:w-64">
							{(p) => (
								<SelectInput
									{...p}
									value={staff}
									onChange={(e) => chooseStaff(e.target.value)}
									disabled={!serviceId || availability.isPending}
								>
									<option value="">Anyone who performs it</option>
									{staffOptions.map((s) => (
										<option key={s.id} value={s.id}>
											{personName(s)}
										</option>
									))}
								</SelectInput>
							)}
						</Field>
					</div>

					<div aria-live="polite">
						{!serviceId ? (
							<p className="text-sm text-ink-subtle">Choose a service to see its open times.</p>
						) : availability.isError ? (
							<p className="text-sm text-error-600 dark:text-error-300">
								Couldn&apos;t load open times: {getErrorMessage(availability.error)}
							</p>
						) : availability.isPending ? (
							<div className="flex flex-col gap-3">
								<Skeleton className="h-5 w-40" />
								<Skeleton className="h-10 w-full" />
							</div>
						) : performers.length === 0 ? (
							<p className="text-sm text-ink-subtle">
								No active staff member performs this service, so it has no open times.
							</p>
						) : staffRows.length === 0 ? (
							<p className="text-sm text-ink-subtle">
								{staffName(Number(staff))} has no open times for this service: they don&apos;t perform it, or
								aren&apos;t active.
							</p>
						) : (
							<ul className="flex flex-col gap-4">
								{staffRows.map((member) => (
									<li key={member.id}>
										<p className="text-sm font-medium text-ink">{personName(member)}</p>
										{member.slots.length === 0 ? (
											<p className="mt-1 text-sm text-ink-subtle">No open times on this day.</p>
										) : (
											<div className="mt-2 flex flex-wrap gap-2">
												{member.slots.map((slot) => {
													const selected =
														!custom && picked?.staffId === member.id && picked.startsAt === slot.startsAt;
													return (
														<button
															key={slot.startsAt}
															type="button"
															aria-pressed={selected}
															aria-label={`${personName(member)} at ${timeOf(slot.startsAt)}`}
															onClick={() => {
																setPicked({ staffId: member.id, startsAt: slot.startsAt });
																setCustomTime("");
															}}
															className={cn(
																"h-9 rounded-lg border px-3 text-sm tabular-nums transition-colors focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-primary-500/30",
																selected
																	? "border-primary-500 bg-primary-50 font-medium text-primary-700 dark:bg-primary-500/15 dark:text-primary-300"
																	: "border-border-default text-ink hover:border-primary-300 hover:bg-primary-50/60 dark:hover:bg-primary-500/[0.07]",
															)}
														>
															{timeOf(slot.startsAt)}
														</button>
													);
												})}
											</div>
										)}
									</li>
								))}
							</ul>
						)}
					</div>

					{serviceId && staff && (
						<Field
							label="Or another time"
							hint={`Outside the open times, booking only checks that ${staffName(Number(staff))} isn't already booked then.`}
							className="sm:w-64"
						>
							{(p) => (
								<TextInput {...p} type="time" value={customTime} onChange={(e) => setCustomTime(e.target.value)} />
							)}
						</Field>
					)}

					<div
						className={cn(
							"flex flex-col gap-3 rounded-lg border px-4 py-3 sm:flex-row sm:items-center sm:justify-between",
							errors.time && !choice ? "border-error-400" : "border-border-default",
						)}
					>
						<p className="text-sm text-ink">
							{choice ? (
								<>
									<span className="font-medium">
										{formatWallDate(choice.startsAt)}, {timeOf(choice.startsAt)}
										{duration != null && `–${timeOf(plusMinutes(choice.startsAt, duration))}`}
									</span>{" "}
									with {staffName(choice.staffId)}
									{isInitialTime && <span className="text-ink-subtle"> (its current time)</span>}
								</>
							) : (
								<span className={errors.time ? "text-error-600 dark:text-error-300" : "text-ink-subtle"}>
									{errors.time ?? "No time picked yet."}
								</span>
							)}
						</p>
						{initial && !isInitialTime && (
							<Button
								size="sm"
								variant="outline"
								onClick={() => {
									setPicked({ staffId: initial.staff.id, startsAt: initial.startsAt });
									setCustomTime("");
								}}
							>
								Keep its current time
							</Button>
						)}
					</div>
				</div>
			</Card>

			<Card title="Notes" description="Anything the staff member should know. Only your team sees these.">
				<Field label="Notes" hint={`Optional, up to ${NOTES_MAX} characters.`}>
					{(p) => (
						<TextareaInput
							{...p}
							value={notes}
							onChange={(e) => setNotes(e.target.value)}
							maxLength={NOTES_MAX}
							rows={4}
						/>
					)}
				</Field>
			</Card>

			<div className="flex justify-end gap-3">
				<Button type="button" variant="outline" onClick={onCancel} disabled={submitting}>
					Cancel
				</Button>
				<Button type="submit" loading={submitting}>
					{submitLabel}
				</Button>
			</div>
		</form>
	);
}

function minutesBetween(startsAt: string, endsAt: string): number {
	return (Date.parse(`${endsAt}:00Z`) - Date.parse(`${startsAt}:00Z`)) / 60_000;
}
