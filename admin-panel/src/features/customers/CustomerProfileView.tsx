"use client";

import { useParams } from "next/navigation";
import Link from "next/link";
import { useCustomer } from "@/lib/resources/customers/customers.hooks";
import Card from "@/components/ui/Card";
import StatTile from "@/components/ui/StatTile";
import { Skeleton } from "@/components/ui/Skeleton";
import { DollarLineIcon, EnvelopeIcon, TaskIcon, TimeIcon } from "@/icons";

function money(value: number) {
	return new Intl.NumberFormat(undefined, { style: "currency", currency: "EUR" }).format(value);
}

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

	const averageTicket = customer.totalVisits > 0 ? customer.totalSpend / customer.totalVisits : 0;

	return (
		<div className="flex flex-col gap-4 md:gap-6">
			<div className="grid grid-cols-1 gap-4 sm:grid-cols-3 md:gap-6">
				<StatTile
					icon={<TaskIcon className="size-5" />}
					label="Total visits"
					value={customer.totalVisits}
				/>
				<StatTile
					icon={<DollarLineIcon className="size-5" />}
					label="Lifetime spend"
					value={money(customer.totalSpend)}
					hint={`${money(averageTicket)} average`}
				/>
				<StatTile
					icon={<TimeIcon className="size-5" />}
					label="Client since"
					value={new Date(customer.firstVisit).getFullYear()}
				/>
			</div>

			<div className="grid grid-cols-1 gap-4 lg:grid-cols-2 md:gap-6">
				<Card title="Contact">
					<ul className="flex flex-col gap-3 text-sm">
						<li className="flex items-center gap-3">
							<EnvelopeIcon className="size-5 shrink-0 text-ink-subtle" />
							<a
								href={`mailto:${customer.email}`}
								className="truncate text-ink hover:text-primary-700 dark:hover:text-primary-300"
							>
								{customer.email}
							</a>
						</li>
						<li className="flex items-center gap-3">
							<span className="w-5 shrink-0 text-center text-ink-subtle">☎</span>
							<a
								href={`tel:${customer.phone.replace(/\s/g, "")}`}
								className="text-ink hover:text-primary-700 dark:hover:text-primary-300"
							>
								{customer.phone}
							</a>
						</li>
					</ul>
				</Card>

				<Card title="History">
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
							<dt className="text-ink-muted">First visit</dt>
							<dd className="text-ink">{formatDate(customer.firstVisit)}</dd>
						</div>
						<div className="flex justify-between gap-4">
							<dt className="text-ink-muted">Last visit</dt>
							<dd className="text-ink">{formatDate(customer.lastVisit)}</dd>
						</div>
					</dl>
				</Card>
			</div>

			<Card title="Why this is sample data">
				<p className="text-sm text-ink-muted">
					Customers are just{" "}
					<code className="rounded bg-neutral-100 px-1 py-0.5 text-xs dark:bg-white/10">users</code>{" "}
					rows with the CUSTOMER role, and the{" "}
					<code className="rounded bg-neutral-100 px-1 py-0.5 text-xs dark:bg-white/10">
						business_customers
					</code>{" "}
					join table is an orphan — no model, no data access layer, no endpoint, and no foreign
					keys. Visit counts and spend depend on appointments, which don&apos;t exist yet either.
				</p>
			</Card>
		</div>
	);
}
