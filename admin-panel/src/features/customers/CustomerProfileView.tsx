"use client";

import { useParams } from "next/navigation";
import Link from "next/link";
import { useCustomer } from "@/lib/resources/customers/customers.hooks";
import type { Customer } from "@/lib/resources/customers/customers.types";
import { useAppointments } from "@/lib/resources/appointments/appointments.hooks";
import { getErrorMessage } from "@/lib/api/errors";
import { formatMoney } from "@/lib/utils/money";
import { formatWallDate, timeOf } from "@/lib/utils/wallClock";
import Card from "@/components/ui/Card";
import { Skeleton } from "@/components/ui/Skeleton";
import { EnvelopeIcon } from "@/icons";
import { appointmentHref } from "@/features/appointments/appointmentLinks";
import { visitSummary } from "./customerVisits";

function formatDate(iso: string) {
	return new Date(iso).toLocaleDateString(undefined, {
		year: "numeric",
		month: "long",
		day: "numeric",
	});
}

export default function CustomerProfileView() {
	const params = useParams<{ id: string }>();
	const id = Number(params.id);
	const { data: customer, isPending } = useCustomer(Number.isNaN(id) ? null : id);

	if (isPending || !customer) return <Skeleton className="h-64 rounded-card" />;

	return (
		<div className="flex flex-col gap-4 md:gap-6">
			<div className="grid grid-cols-1 gap-4 lg:grid-cols-2 md:gap-6">
				<Card title="Contact">
					<ul className="flex flex-col gap-3 text-sm">
						<li className="flex items-center gap-3">
							<EnvelopeIcon className="size-5 shrink-0 text-ink-subtle" />
							{customer.email ? (
								<a
									href={`mailto:${customer.email}`}
									className="truncate text-ink hover:text-primary-700 dark:hover:text-primary-300"
								>
									{customer.email}
								</a>
							) : (
								<span className="text-ink-subtle">No email on file</span>
							)}
						</li>
						<li className="flex items-center gap-3">
							<span className="w-5 shrink-0 text-center text-ink-subtle">☎</span>
							{customer.phone ? (
								<a
									href={`tel:${customer.phone.replace(/\s/g, "")}`}
									className="text-ink hover:text-primary-700 dark:hover:text-primary-300"
								>
									{customer.phone}
								</a>
							) : (
								<span className="text-ink-subtle">No phone on file</span>
							)}
						</li>
					</ul>
				</Card>

				<Card title="Record">
					<dl className="flex flex-col gap-3 text-sm">
						<div className="flex justify-between gap-4">
							<dt className="text-ink-muted">Salon</dt>
							<dd>
								<Link
									href={`/businesses/${customer.businessId}`}
									className="text-ink hover:text-primary-700 dark:hover:text-primary-300"
								>
									{customer.businessName}
								</Link>
							</dd>
						</div>
						<div className="flex justify-between gap-4">
							<dt className="text-ink-muted">Client since</dt>
							<dd className="text-ink">{formatDate(customer.createdAt)}</dd>
						</div>
						{customer.updatedAt && (
							<div className="flex justify-between gap-4">
								<dt className="text-ink-muted">Last updated</dt>
								<dd className="text-ink">{formatDate(customer.updatedAt)}</dd>
							</div>
						)}
						<div className="flex justify-between gap-4">
							<dt className="text-ink-muted">Marketing</dt>
							<dd className="text-ink">{customer.marketingConsent ? "Opted in" : "Not opted in"}</dd>
						</div>
						<div className="flex justify-between gap-4">
							<dt className="text-ink-muted">Online account</dt>
							<dd className="text-ink">{customer.userId != null ? "Linked" : "None"}</dd>
						</div>
					</dl>
				</Card>
			</div>

			<VisitsCard customer={customer} />
		</div>
	);
}

/** Counted from completed appointments; the Appointments tab lists every one. */
function VisitsCard({ customer }: { customer: Customer }) {
	const { data, isPending, isError, error } = useAppointments(customer.businessId, { customerId: customer.id });

	if (isPending) return <Skeleton className="h-40 rounded-card" />;

	if (isError) {
		return (
			<Card title="Visits and spend">
				<p className="text-sm text-error-600 dark:text-error-300">
					Couldn&apos;t load their appointments: {getErrorMessage(error)}
				</p>
			</Card>
		);
	}

	const { visits, spent, currency, lastVisit, next } = visitSummary(data);

	return (
		<Card title="Visits and spend" description="Completed appointments only, at the price each was booked at.">
			<dl className="flex flex-col gap-3 text-sm">
				<div className="flex justify-between gap-4">
					<dt className="text-ink-muted">Visits</dt>
					<dd className="tabular-nums text-ink">{visits}</dd>
				</div>
				<div className="flex justify-between gap-4">
					<dt className="text-ink-muted">Spent</dt>
					<dd className="tabular-nums text-ink">
						{spent != null && currency ? formatMoney(spent, currency) : "—"}
					</dd>
				</div>
				<div className="flex justify-between gap-4">
					<dt className="text-ink-muted">Last visit</dt>
					<dd className="text-ink">{lastVisit ? formatWallDate(lastVisit) : "None yet"}</dd>
				</div>
				<div className="flex justify-between gap-4">
					<dt className="text-ink-muted">Next appointment</dt>
					<dd>
						{next ? (
							<Link
								href={appointmentHref(next)}
								className="text-ink hover:text-primary-700 dark:hover:text-primary-300"
							>
								{formatWallDate(next.startsAt)}, {timeOf(next.startsAt)} · {next.service.name}
							</Link>
						) : (
							<span className="text-ink">None booked</span>
						)}
					</dd>
				</div>
			</dl>
		</Card>
	);
}
