"use client";

import { useParams } from "next/navigation";
import Link from "next/link";
import { useEmployee } from "@/lib/resources/employees/employees.hooks";
import Card from "@/components/ui/Card";
import StatTile from "@/components/ui/StatTile";
import { Skeleton } from "@/components/ui/Skeleton";
import { CalenderIcon, EnvelopeIcon, TimeIcon } from "@/icons";

export default function EmployeeProfileView() {
	const params = useParams<{ id: string }>();
	const id = Number(params.id);
	const { data: employee, isPending } = useEmployee(Number.isNaN(id) ? null : id);

	if (isPending || !employee) return <Skeleton className="h-64 rounded-card" />;

	const workingDays = employee.workingHours.filter((d) => d.start !== null);
	const weeklyHours = workingDays.reduce((sum, d) => {
		if (!d.start || !d.end) return sum;
		const [sh, sm] = d.start.split(":").map(Number);
		const [eh, em] = d.end.split(":").map(Number);
		return sum + (eh * 60 + em - (sh * 60 + sm)) / 60;
	}, 0);

	return (
		<div className="flex flex-col gap-4 md:gap-6">
			<div className="grid grid-cols-1 gap-4 sm:grid-cols-3 md:gap-6">
				<StatTile
					icon={<CalenderIcon className="size-5" />}
					label="Working days"
					value={`${workingDays.length} / 7`}
				/>
				<StatTile
					icon={<TimeIcon className="size-5" />}
					label="Scheduled hours"
					value={`${Math.round(weeklyHours)} h`}
					hint="Per week"
				/>
				<StatTile
					icon={<CalenderIcon className="size-5" />}
					label="Bookings this week"
					value={employee.appointmentsThisWeek}
				/>
			</div>

			<div className="grid grid-cols-1 gap-4 lg:grid-cols-2 md:gap-6">
				<Card title="Contact">
					<ul className="flex flex-col gap-3 text-sm">
						<li className="flex items-center gap-3">
							<EnvelopeIcon className="size-5 shrink-0 text-ink-subtle" />
							<a
								href={`mailto:${employee.email}`}
								className="truncate text-ink hover:text-primary-700 dark:hover:text-primary-300"
							>
								{employee.email}
							</a>
						</li>
					</ul>
				</Card>

				<Card title="Placement">
					<dl className="flex flex-col gap-3 text-sm">
						<div className="flex justify-between gap-4">
							<dt className="text-ink-muted">Salon</dt>
							<dd>
								<Link
									href={`/businesses/${employee.businessId}`}
									className="text-ink hover:text-primary-700 dark:hover:text-primary-300"
								>
									{employee.businessName}
								</Link>
							</dd>
						</div>
						<div className="flex justify-between gap-4">
							<dt className="text-ink-muted">Title</dt>
							<dd className="text-ink">{employee.title}</dd>
						</div>
						<div className="flex justify-between gap-4">
							<dt className="text-ink-muted">Joined</dt>
							<dd className="text-ink">
								{new Date(employee.hiredAt).toLocaleDateString(undefined, {
									year: "numeric",
									month: "long",
									day: "numeric",
								})}
							</dd>
						</div>
					</dl>
				</Card>
			</div>

			<Card title="Why this is sample data">
				<p className="text-sm text-ink-muted">
					The <code className="rounded bg-neutral-100 px-1 py-0.5 text-xs dark:bg-white/10">staff</code>{" "}
					table exists in the database, but its data access layer is entirely stubbed out and
					isn&apos;t registered as a Spring bean — so nothing can read or write a roster.
					Creating staff accounts, on the other hand, is real: that writes to{" "}
					<code className="rounded bg-neutral-100 px-1 py-0.5 text-xs dark:bg-white/10">users</code>{" "}
					through a correctly scoped endpoint.
				</p>
			</Card>
		</div>
	);
}
