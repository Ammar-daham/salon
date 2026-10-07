"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import type { Id } from "@/lib/api/types";
import { getErrorMessage } from "@/lib/api/errors";
import { useDataTable } from "@/lib/table/useDataTable";
import { useAppointments } from "@/lib/resources/appointments/appointments.hooks";
import {
	APPOINTMENT_STATUSES,
	APPOINTMENT_STATUS_LABELS,
	APPOINTMENT_STATUS_TONE,
	personName,
	type Appointment,
	type AppointmentFilter,
} from "@/lib/resources/appointments/appointments.types";
import { formatMoney } from "@/lib/utils/money";
import { addDays, browserTimeZone, formatWallDate, timeOf, todayOn } from "@/lib/utils/wallClock";

import DataTable, { type Column } from "@/components/ui/DataTable";
import SearchInput from "@/components/ui/SearchInput";
import Pagination from "@/components/ui/Pagination";
import EmptyState from "@/components/ui/EmptyState";
import StatusBadge from "@/components/ui/StatusBadge";
import Button from "@/components/ui/button/Button";
import { SelectInput, TextInput } from "@/components/ui/form/Field";
import { ErrorIcon, TaskIcon } from "@/icons";
import { appointmentHref } from "./appointmentLinks";

type HideableColumn = "customer" | "staff";

interface AppointmentListProps {
	/** One salon, or null for every salon (a SUPER_ADMIN's platform view). */
	businessId: Id | null;
	/** Narrow the list on the server to one staff member's or one client's appointments. */
	staffId?: Id;
	customerId?: Id;
	/**
	 * "dates" reads a date range, starting with the next seven days, soonest first.
	 * "history" reads everything, newest first, for one client's or staff member's record.
	 */
	mode?: "dates" | "history";
	/** Columns the page already says, e.g. the client on their own history. */
	hide?: HideableColumn[];
	/** Shown under "No appointments yet", e.g. how to book one. */
	emptyHint?: string;
}

/**
 * The appointments table every page shares. Dates go to the server, which filters by start on
 * each salon's clock; status, staff and the search box filter what came back.
 */
export default function AppointmentList({
	businessId,
	staffId,
	customerId,
	mode = "dates",
	hide = [],
	emptyHint,
}: AppointmentListProps) {
	const router = useRouter();
	// The browser's today: close enough for a starting range, and one range may span salons on different clocks.
	const [from, setFrom] = useState(() => todayOn(browserTimeZone()));
	const [to, setTo] = useState(() => addDays(todayOn(browserTimeZone()), 6));
	const [status, setStatus] = useState("ALL");
	const [staffFilter, setStaffFilter] = useState("ALL");

	const datesValid = mode === "history" || (from !== "" && to !== "" && from <= to);
	const filter: AppointmentFilter =
		mode === "dates" ? { from, to, staffId, customerId } : { staffId, customerId };
	const { data, isPending, isError, error, refetch } = useAppointments(businessId, filter, datesValid);

	const crossBusiness = businessId == null;
	const showCustomer = !hide.includes("customer");
	const showStaff = !hide.includes("staff");
	const pickStaff = showStaff && staffId == null;

	const staffOptions = useMemo(() => {
		const seen = new Map<Id, string>();
		data.forEach((a) => seen.set(a.staff.id, personName(a.staff)));
		return [...seen.entries()].sort((a, b) => a[1].localeCompare(b[1]));
	}, [data]);

	const filters = useMemo(
		() => [
			(a: Appointment) => status === "ALL" || a.status === status,
			(a: Appointment) => staffFilter === "ALL" || String(a.staff.id) === staffFilter,
		],
		[status, staffFilter],
	);

	const table = useDataTable<Appointment>({
		rows: data,
		searchAccessor: (a) =>
			`${personName(a.customer)} ${personName(a.staff)} ${a.service.name} ${a.businessName}`,
		sortAccessor: (a, key) => {
			switch (key) {
				case "when": return a.startsAt;
				case "customer": return `${a.customer.lastName} ${a.customer.firstName}`;
				case "service": return a.service.name;
				case "staff": return `${a.staff.lastName} ${a.staff.firstName}`;
				case "business": return a.businessName;
				default: return null;
			}
		},
		initialSort: { key: "when", direction: mode === "history" ? "desc" : "asc" },
		filters,
		pageSize: 15,
	});
	const narrowed = table.search.trim() !== "" || status !== "ALL" || staffFilter !== "ALL";

	function clearFilters() {
		table.setSearch("");
		setStatus("ALL");
		setStaffFilter("ALL");
	}

	const columns: Column<Appointment>[] = [
		{
			key: "when",
			header: "When",
			sortable: true,
			render: (a) => (
				<Link href={appointmentHref(a)} className="group block whitespace-nowrap">
					<span className="block font-medium text-ink group-hover:text-primary-700 dark:group-hover:text-primary-300">
						{formatWallDate(a.startsAt, { weekday: "short", day: "numeric", month: "short", year: "numeric" })}
					</span>
					<span className="block text-xs tabular-nums text-ink-subtle">
						{timeOf(a.startsAt)}–{timeOf(a.endsAt)}
					</span>
				</Link>
			),
		},
		...(showCustomer
			? [
					{
						key: "customer",
						header: "Client",
						sortable: true,
						render: (a: Appointment) => personName(a.customer),
					} satisfies Column<Appointment>,
			  ]
			: []),
		{ key: "service", header: "Service", sortable: true, render: (a) => a.service.name },
		...(showStaff
			? [
					{
						key: "staff",
						header: "With",
						sortable: true,
						render: (a: Appointment) => <span className="text-ink-muted">{personName(a.staff)}</span>,
					} satisfies Column<Appointment>,
			  ]
			: []),
		...(crossBusiness
			? [
					{
						key: "business",
						header: "Salon",
						sortable: true,
						render: (a: Appointment) => <span className="text-ink-muted">{a.businessName}</span>,
					} satisfies Column<Appointment>,
			  ]
			: []),
		{
			key: "status",
			header: "Status",
			render: (a) => (
				<StatusBadge tone={APPOINTMENT_STATUS_TONE[a.status]}>{APPOINTMENT_STATUS_LABELS[a.status]}</StatusBadge>
			),
		},
		{
			key: "price",
			header: "Price",
			align: "right",
			render: (a) => <span className="tabular-nums text-ink-muted">{formatMoney(a.price, a.currency)}</span>,
		},
		{
			key: "actions",
			header: "",
			align: "right",
			render: (a) => (
				<Button size="sm" variant="outline" onClick={() => router.push(appointmentHref(a))}>
					View
				</Button>
			),
		},
	];

	return (
		<>
			<div className="mb-4 flex flex-col gap-3 lg:flex-row lg:flex-wrap lg:items-center">
				{mode === "dates" && (
					<div className="flex items-center gap-2">
						<TextInput
							type="date"
							aria-label="From"
							value={from}
							max={to || undefined}
							onChange={(e) => setFrom(e.target.value)}
							className="w-40"
						/>
						<span className="text-ink-subtle" aria-hidden="true">
							–
						</span>
						<TextInput
							type="date"
							aria-label="Until"
							value={to}
							min={from || undefined}
							onChange={(e) => setTo(e.target.value)}
							className="w-40"
						/>
					</div>
				)}
				<SearchInput
					value={table.search}
					onChange={table.setSearch}
					placeholder={showCustomer ? "Search by client, service or staff…" : "Search by service or staff…"}
					aria-label="Search appointments"
					className="lg:max-w-xs"
				/>
				<SelectInput
					aria-label="Filter by status"
					value={status}
					onChange={(e) => setStatus(e.target.value)}
					className="lg:w-44"
				>
					<option value="ALL">Any status</option>
					{APPOINTMENT_STATUSES.map((s) => (
						<option key={s} value={s}>
							{APPOINTMENT_STATUS_LABELS[s]}
						</option>
					))}
				</SelectInput>
				{pickStaff && (
					<SelectInput
						aria-label="Filter by staff member"
						value={staffFilter}
						onChange={(e) => setStaffFilter(e.target.value)}
						className="lg:w-52"
					>
						<option value="ALL">Anyone</option>
						{staffOptions.map(([id, name]) => (
							<option key={id} value={id}>
								{name}
							</option>
						))}
					</SelectInput>
				)}
				{datesValid && !isPending && !isError && (
					<p className="text-sm text-ink-subtle lg:ml-auto">
						{table.total} {table.total === 1 ? "appointment" : "appointments"}
					</p>
				)}
			</div>

			{!datesValid ? (
				<EmptyState
					icon={<TaskIcon className="size-6" />}
					title="Pick a date range"
					description="Choose a start date and an end date on or after it."
				/>
			) : isError ? (
				<EmptyState
					icon={<ErrorIcon className="size-6" />}
					title="Couldn't load appointments"
					description={getErrorMessage(error)}
					action={<Button variant="outline" onClick={() => refetch()}>Try again</Button>}
				/>
			) : (
				<>
					<DataTable
						columns={columns}
						rows={table.rows}
						rowKey={(a) => a.id}
						loading={isPending}
						sort={table.sort}
						onSort={table.toggleSort}
						empty={
							<EmptyState
								icon={<TaskIcon className="size-6" />}
								title={
									narrowed
										? "No appointments match"
										: mode === "dates"
											? "No appointments in these dates"
											: "No appointments yet"
								}
								description={
									narrowed
										? "Try a different search term, or clear the filters."
										: mode === "dates"
											? "Try a wider date range."
											: emptyHint
								}
								action={
									narrowed && (
										<Button variant="outline" onClick={clearFilters}>
											Clear filters
										</Button>
									)
								}
							/>
						}
					/>

					{!isPending && table.total > 0 && (
						<div className="mt-px rounded-b-card border border-t-0 border-border-default bg-surface-raised">
							<Pagination
								page={table.page}
								pageCount={table.pageCount}
								total={table.total}
								pageSize={table.pageSize}
								onPageChange={table.setPage}
							/>
						</div>
					)}
				</>
			)}
		</>
	);
}
