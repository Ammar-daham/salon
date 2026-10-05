"use client";

import { useParams } from "next/navigation";
import Link from "next/link";
import { useEmployee } from "@/lib/resources/employees/employees.hooks";
import Card from "@/components/ui/Card";
import { Skeleton } from "@/components/ui/Skeleton";
import { EnvelopeIcon } from "@/icons";

export default function EmployeeProfileView() {
	const params = useParams<{ id: string }>();
	const id = Number(params.id);
	const { data: employee, isPending } = useEmployee(Number.isNaN(id) ? null : id);

	if (isPending || !employee) return <Skeleton className="h-64 rounded-card" />;

	return (
		<div className="flex flex-col gap-4 md:gap-6">
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

			<Card title="What's still not real">
				<p className="text-sm text-ink-muted">
					This profile is live - title, salon and hire date all come from the real{" "}
					<code className="rounded bg-neutral-100 px-1 py-0.5 text-xs dark:bg-white/10">staff</code>{" "}
					table. Working hours and bookings aren&apos;t shown here because neither exists on the
					backend yet: there is no business-hours/schedule table and no appointments table. See the
					Schedule and Appointments tabs.
				</p>
			</Card>
		</div>
	);
}
