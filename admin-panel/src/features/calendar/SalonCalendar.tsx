"use client";

import { useMemo, useRef, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import FullCalendar from "@fullcalendar/react";
import timeGridPlugin from "@fullcalendar/timegrid";
import interactionPlugin from "@fullcalendar/interaction";
import type { DatesSetArg, EventContentArg, EventMountArg } from "@fullcalendar/core";
import { useAuth } from "@/context/AuthContext";
import { can } from "@/lib/auth/permissions";
import type { Id } from "@/lib/api/types";
import { getErrorMessage } from "@/lib/api/errors";
import { useAppointments } from "@/lib/resources/appointments/appointments.hooks";
import {
	APPOINTMENT_STATUS_LABELS,
	personName,
	type Appointment,
} from "@/lib/resources/appointments/appointments.types";
import { useEmployees } from "@/lib/resources/employees/employees.hooks";
import { employeeFullName } from "@/lib/resources/employees/employees.types";
import { useOpeningHours, useWorkingHours } from "@/lib/resources/hours/hours.hooks";
import { formatWallDate, timeOf, todayOn, wallClockNow } from "@/lib/utils/wallClock";
import { cn } from "@/lib/utils/cn";
import EmptyState from "@/components/ui/EmptyState";
import Button from "@/components/ui/button/Button";
import { Skeleton } from "@/components/ui/Skeleton";
import { ChevronLeftIcon, ErrorIcon, PlusIcon } from "@/icons";
import { appointmentHref, bookingHref } from "@/features/appointments/appointmentLinks";
import { dateRange, staffColour, toBusinessHours, toEvent, visibleHours, wallDateTime } from "./calendarLayout";

type View = "timeGridDay" | "timeGridWeek";

const VIEWS: { id: View; label: string }[] = [
	{ id: "timeGridDay", label: "Day" },
	{ id: "timeGridWeek", label: "Week" },
];

const TIME_FORMAT = { hour: "2-digit", minute: "2-digit", hour12: false } as const;

/** What the calendar is showing, as FullCalendar last reported it. */
interface Shown {
	view: View;
	from: string;
	to: string;
	title: string;
}

interface StaffMember {
	id: Id;
	name: string;
	colour: string;
}

/**
 * A salon's appointments on a time grid, on the salon's clock (see calendarLayout.ts), for the whole
 * team in each staff member's colour or for one of them. The shaded hours are when the salon is
 * closed, or when that staff member has no shift.
 */
export default function SalonCalendar({ businessId, initialStaffId }: { businessId: Id; initialStaffId: Id | null }) {
	const router = useRouter();
	const { user } = useAuth();
	const calendar = useRef<FullCalendar>(null);
	const [shown, setShown] = useState<Shown | null>(null);
	const [staffId, setStaffId] = useState<Id | null>(initialStaffId);
	const [showCancelled, setShowCancelled] = useState(false);

	const hours = useOpeningHours(businessId);
	const appointments = useAppointments(businessId, { from: shown?.from, to: shown?.to }, shown != null);
	// Names come from /users, which an EMPLOYEE can't read: they get the staff seen in the appointments.
	const employees = useEmployees(businessId, can(user, "employee:list"));
	const shifts = useWorkingHours(staffId != null ? businessId : null, staffId);
	const canBook = can(user, "appointment:create");

	const staff = useMemo<StaffMember[]>(() => {
		const booked = new Map<Id, string>();
		appointments.data.forEach((a) => booked.set(a.staff.id, personName(a.staff)));
		const members = new Map<Id, StaffMember>();
		// The one picked stays on offer even when they've left and have nothing booked this week.
		employees.data
			.filter((e) => e.isActive || booked.has(e.id) || e.id === staffId)
			.forEach((e) => members.set(e.id, { id: e.id, name: employeeFullName(e), colour: staffColour(e.id, e.calendarColour) }));
		booked.forEach((name, id) => {
			if (!members.has(id)) members.set(id, { id, name, colour: staffColour(id, null) });
		});
		return [...members.values()].sort((a, b) => a.name.localeCompare(b.name));
	}, [appointments.data, employees.data, staffId]);

	const visible = useMemo(
		() =>
			appointments.data.filter(
				(a) => (staffId == null || a.staff.id === staffId) && (showCancelled || a.status !== "CANCELLED"),
			),
		[appointments.data, staffId, showCancelled],
	);
	const events = useMemo(() => {
		const colours = new Map(staff.map((s) => [s.id, s.colour]));
		return visible.map((a) => toEvent(a, colours.get(a.staff.id) ?? staffColour(a.staff.id, null)));
	}, [visible, staff]);

	if (hours.isError) {
		return (
			<EmptyState
				icon={<ErrorIcon className="size-6" />}
				title="Couldn't load the calendar"
				description={getErrorMessage(hours.error)}
				action={
					<Button variant="outline" onClick={() => hours.refetch()}>
						Try again
					</Button>
				}
			/>
		);
	}

	if (!hours.data) return <Skeleton className="h-[36rem] rounded-card" />;

	const { timezone, intervals } = hours.data;
	// A staff member's shifts once they've loaded; the salon's hours until then.
	const ownShifts = staffId != null ? shifts.data?.intervals : undefined;
	const week = ownShifts ?? intervals;
	const grid = visibleHours(intervals, visible);
	const api = () => calendar.current?.getApi();
	const chosen = staff.find((s) => s.id === staffId);

	return (
		<div className="flex flex-col gap-4">
			<div className="flex flex-wrap items-center gap-2" role="group" aria-label="Show appointments for">
				<Chip pressed={staffId == null} onClick={() => setStaffId(null)}>
					Everyone
				</Chip>
				{staff.map((s) => (
					<Chip key={s.id} pressed={staffId === s.id} onClick={() => setStaffId(s.id)}>
						<span className="size-2.5 shrink-0 rounded-full" style={{ backgroundColor: s.colour }} aria-hidden="true" />
						{s.name}
					</Chip>
				))}
				<Chip pressed={showCancelled} onClick={() => setShowCancelled((on) => !on)} className="sm:ml-auto">
					Show cancelled
				</Chip>
			</div>

			<section className="rounded-card border border-border-default bg-surface-raised shadow-xs">
				<div className="flex flex-col gap-3 border-b border-border-default px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
					<div className="flex items-center gap-2">
						<Button
							size="sm"
							variant="outline"
							aria-label={shown?.view === "timeGridDay" ? "Previous day" : "Previous week"}
							startIcon={<ChevronLeftIcon className="size-4" />}
							onClick={() => api()?.prev()}
						/>
						<Button
							size="sm"
							variant="outline"
							aria-label={shown?.view === "timeGridDay" ? "Next day" : "Next week"}
							startIcon={<ChevronLeftIcon className="size-4 rotate-180" />}
							onClick={() => api()?.next()}
						/>
						<Button size="sm" variant="outline" onClick={() => api()?.today()}>
							Today
						</Button>
						<h2 className="ml-2 text-base font-semibold text-ink" aria-live="polite">
							{shown?.title}
						</h2>
						{appointments.isFetching && <span className="text-sm text-ink-subtle">Loading…</span>}
					</div>

					<div className="flex items-center gap-3">
						<div role="group" aria-label="View" className="flex rounded-lg bg-neutral-100 p-0.5 dark:bg-white/5">
							{VIEWS.map((v) => (
								<button
									key={v.id}
									type="button"
									aria-pressed={shown?.view === v.id}
									onClick={() => api()?.changeView(v.id)}
									className={cn(
										"rounded-md px-4 py-1.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-primary-500/30",
										shown?.view === v.id ? "bg-surface-raised text-ink shadow-xs" : "text-ink-muted hover:text-ink",
									)}
								>
									{v.label}
								</button>
							))}
						</div>
						{canBook && (
							<Button
								size="sm"
								startIcon={<PlusIcon className="size-4" />}
								onClick={() =>
									router.push(
										bookingHref({
											businessId,
											staffId: staffId ?? undefined,
											date: shown?.view === "timeGridDay" ? shown.from : undefined,
										}),
									)
								}
							>
								Book
							</Button>
						)}
					</div>
				</div>

				{appointments.isError && (
					<div role="alert" className="flex flex-wrap items-center gap-3 border-b border-border-default px-4 py-3 text-sm">
						<span className="text-error-600 dark:text-error-300">
							Couldn&apos;t load appointments: {getErrorMessage(appointments.error)}
						</span>
						<Button size="sm" variant="outline" onClick={() => appointments.refetch()}>
							Try again
						</Button>
					</div>
				)}

				<div className="p-2 sm:p-4">
					<FullCalendar
						ref={calendar}
						plugins={[timeGridPlugin, interactionPlugin]}
						// A phone is too narrow for seven columns.
						initialView={
							typeof window !== "undefined" && window.matchMedia("(max-width: 639px)").matches
								? "timeGridDay"
								: "timeGridWeek"
						}
						initialDate={todayOn(timezone)}
						timeZone="UTC"
						now={() => wallClockNow(timezone)}
						nowIndicator
						firstDay={1}
						headerToolbar={false}
						allDaySlot={false}
						height="auto"
						navLinks
						slotMinTime={grid.slotMinTime}
						slotMaxTime={grid.slotMaxTime}
						slotLabelFormat={TIME_FORMAT}
						eventTimeFormat={TIME_FORMAT}
						dayHeaderFormat={{ weekday: "short", day: "numeric", month: "short" }}
						businessHours={toBusinessHours(week)}
						events={events}
						eventContent={(arg) => <EventCard arg={arg} showStaff={staffId == null} />}
						eventDidMount={(arg: EventMountArg) => {
							arg.el.title = describe(arg.event.extendedProps.appointment as Appointment);
						}}
						eventClick={(arg) => router.push(appointmentHref(arg.event.extendedProps.appointment as Appointment))}
						dateClick={
							canBook
								? (arg) =>
										router.push(
											bookingHref({
												businessId,
												staffId: staffId ?? undefined,
												date: wallDateTime(arg.date).slice(0, 10),
											}),
										)
								: undefined
						}
						datesSet={(arg: DatesSetArg) => {
							const next: Shown = { view: arg.view.type as View, title: arg.view.title, ...dateRange(arg.start, arg.end) };
							setShown((prev) =>
								prev && prev.view === next.view && prev.from === next.from && prev.to === next.to ? prev : next,
							);
						}}
					/>
				</div>
			</section>

			<p className="text-sm text-ink-subtle">
				Times are on the salon&apos;s clock, {timezone.replaceAll("_", " ")}. Shaded hours are{" "}
				{ownShifts ? `outside ${chosen ? `${chosen.name}'s` : "their"} shifts` : "outside opening hours"}
				{week.length === 0 && (ownShifts ? ", and they have none set" : ", and none are set")}.
				{canBook && " Click an empty time to book on that day."}
			</p>
		</div>
	);
}

/** A toggle in the row above the calendar. */
function Chip({
	pressed,
	onClick,
	className,
	children,
}: {
	pressed: boolean;
	onClick: () => void;
	className?: string;
	children: ReactNode;
}) {
	return (
		<button
			type="button"
			aria-pressed={pressed}
			onClick={onClick}
			className={cn(
				"inline-flex h-8 items-center gap-2 rounded-full border px-3 text-sm transition-colors focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-primary-500/30",
				pressed
					? "border-primary-500 bg-primary-50 font-medium text-primary-700 dark:bg-primary-500/15 dark:text-primary-300"
					: "border-border-default text-ink hover:border-primary-300 hover:bg-primary-50/60 dark:hover:bg-primary-500/[0.07]",
				className,
			)}
		>
			{children}
		</button>
	);
}

/** As much as fits: a short appointment shows its first lines, and the title tooltip has the rest. */
function EventCard({ arg, showStaff }: { arg: EventContentArg; showStaff: boolean }) {
	const a = arg.event.extendedProps.appointment as Appointment;
	return (
		<div className="flex h-full flex-col overflow-hidden px-1 py-0.5 text-xs leading-snug">
			<span className="tabular-nums text-ink-muted">
				{timeOf(a.startsAt)}–{timeOf(a.endsAt)}
				{a.status !== "BOOKED" && ` · ${APPOINTMENT_STATUS_LABELS[a.status]}`}
			</span>
			<span className={cn("truncate font-medium", a.status === "CANCELLED" && "line-through")}>
				{personName(a.customer)}
			</span>
			<span className="truncate">{a.service.name}</span>
			{showStaff && <span className="truncate text-ink-muted">with {personName(a.staff)}</span>}
		</div>
	);
}

function describe(a: Appointment): string {
	return `${formatWallDate(a.startsAt)}, ${timeOf(a.startsAt)}–${timeOf(a.endsAt)}: ${a.service.name} for ${personName(a.customer)} with ${personName(a.staff)} (${APPOINTMENT_STATUS_LABELS[a.status]})`;
}
