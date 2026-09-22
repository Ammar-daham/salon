"use client";

import { useParams } from "next/navigation";
import { useBusiness } from "@/lib/resources/businesses/businesses.hooks";
import Card from "@/components/ui/Card";
import StatTile from "@/components/ui/StatTile";
import { Skeleton } from "@/components/ui/Skeleton";
import { ListIcon, DollarLineIcon, TimeIcon } from "@/icons";

function money(value: number) {
	return new Intl.NumberFormat(undefined, { style: "currency", currency: "EUR" }).format(value);
}

export default function BusinessOverviewView() {
	// useParams() is a plain object on the client — only the server `params` prop
	// is a Promise.
	const params = useParams<{ id: string }>();
	const id = Number(params.id);
	const { data: business, isPending } = useBusiness(Number.isNaN(id) ? null : id);

	if (isPending || !business) {
		return (
			<div className="grid grid-cols-1 gap-4 md:grid-cols-3 md:gap-6">
				{[0, 1, 2].map((i) => (
					<Skeleton key={i} className="h-32 rounded-card" />
				))}
			</div>
		);
	}

	const active = business.services.filter((s) => s.isActive);
	const avgPrice = active.length
		? active.reduce((sum, s) => sum + s.price, 0) / active.length
		: 0;
	const avgDuration = active.length
		? Math.round(active.reduce((sum, s) => sum + s.durationMinutes, 0) / active.length)
		: 0;

	const address = business.addresses[0];

	// Genuinely live: derived from whether the profile fields are actually filled.
	const completeness = [
		Boolean(business.description?.trim()),
		Boolean(business.image?.trim()),
		business.addresses.length > 0,
		business.contacts.length > 0,
		business.services.length > 0,
	];
	const completedCount = completeness.filter(Boolean).length;
	const completePct = Math.round((completedCount / completeness.length) * 100);

	return (
		<div className="flex flex-col gap-4 md:gap-6">
			<div className="grid grid-cols-1 gap-4 sm:grid-cols-3 md:gap-6">
				<StatTile
					icon={<ListIcon className="size-5" />}
					label="Active services"
					value={active.length}
					hint={
						business.services.length !== active.length
							? `${business.services.length - active.length} inactive`
							: undefined
					}
				/>
				<StatTile
					icon={<DollarLineIcon className="size-5" />}
					label="Average price"
					value={active.length ? money(avgPrice) : "—"}
				/>
				<StatTile
					icon={<TimeIcon className="size-5" />}
					label="Average duration"
					value={active.length ? `${avgDuration} min` : "—"}
				/>
			</div>

			<div className="grid grid-cols-1 gap-4 lg:grid-cols-2 md:gap-6">
				<Card title="Location">
					{address ? (
						<address className="not-italic text-sm leading-relaxed text-ink">
							{address.street}
							<br />
							{address.postalCode ? `${address.postalCode} ` : ""}
							{address.city}
							<br />
							<span className="text-ink-muted">{address.country}</span>
						</address>
					) : (
						<p className="text-sm text-ink-muted">No address on file.</p>
					)}
				</Card>

				<Card title="Contacts">
					{business.contacts.length > 0 ? (
						<ul className="flex flex-col gap-3">
							{business.contacts.map((c) => (
								<li key={c.id} className="flex items-center justify-between gap-4">
									<span className="text-xs font-medium uppercase tracking-wide text-ink-subtle">
										{c.type}
									</span>
									<span className="truncate text-sm text-ink">{c.value}</span>
								</li>
							))}
						</ul>
					) : (
						<p className="text-sm text-ink-muted">No contacts on file.</p>
					)}
				</Card>
			</div>

			<Card
				title="Profile completeness"
				description="Filling these in makes the salon easier to find and book."
			>
				<div className="flex items-center gap-4">
					<div
						className="h-2 flex-1 overflow-hidden rounded-full bg-neutral-200 dark:bg-white/10"
						role="progressbar"
						aria-valuenow={completePct}
						aria-valuemin={0}
						aria-valuemax={100}
						aria-label="Profile completeness"
					>
						<div
							className="h-full rounded-full bg-primary-500 transition-all"
							style={{ width: `${completePct}%` }}
						/>
					</div>
					<span className="shrink-0 text-sm font-medium tabular-nums text-ink">
						{completePct}%
					</span>
				</div>
				<ul className="mt-4 grid grid-cols-1 gap-2 text-sm sm:grid-cols-2">
					{[
						["Description", completeness[0]],
						["Image", completeness[1]],
						["Address", completeness[2]],
						["Contacts", completeness[3]],
						["Services", completeness[4]],
					].map(([label, done]) => (
						<li key={label as string} className="flex items-center gap-2">
							<span
								className={
									done
										? "size-1.5 rounded-full bg-success-500"
										: "size-1.5 rounded-full bg-neutral-300 dark:bg-white/20"
								}
								aria-hidden="true"
							/>
							<span className={done ? "text-ink" : "text-ink-subtle"}>
								{label as string}
								{!done && " — missing"}
							</span>
						</li>
					))}
				</ul>
			</Card>
		</div>
	);
}
