"use client";

import { useParams } from "next/navigation";
import Link from "next/link";
import { useCustomer } from "@/lib/resources/customers/customers.hooks";
import Card from "@/components/ui/Card";
import { Skeleton } from "@/components/ui/Skeleton";
import { EnvelopeIcon } from "@/icons";

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

			<Card title="Visits and spend">
				<p className="text-sm text-ink-muted">
					Visit counts, lifetime spend and last-visit dates come from appointments, which the
					backend doesn&apos;t have yet. They&apos;ll appear here once booking lands.
				</p>
			</Card>
		</div>
	);
}
