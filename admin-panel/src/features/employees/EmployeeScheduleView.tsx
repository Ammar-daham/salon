"use client";

import { useParams } from "next/navigation";
import { useEmployee } from "@/lib/resources/employees/employees.hooks";
import Card from "@/components/ui/Card";
import { Skeleton } from "@/components/ui/Skeleton";
import { cn } from "@/lib/utils/cn";

/** Minutes from midnight, used to position the bars on a shared 06:00–22:00 axis. */
const DAY_START = 6 * 60;
const DAY_END = 22 * 60;

function toMinutes(time: string) {
	const [h, m] = time.split(":").map(Number);
	return h * 60 + m;
}

export default function EmployeeScheduleView() {
	const params = useParams<{ id: string }>();
	const id = Number(params.id);
	const { data: employee, isPending } = useEmployee(Number.isNaN(id) ? null : id);

	if (isPending || !employee) return <Skeleton className="h-64 rounded-card" />;

	const span = DAY_END - DAY_START;

	return (
		<div className="flex flex-col gap-4 md:gap-6">
			<Card
				title="Weekly working hours"
				description="The hours this person is available to take bookings."
			>
				<div className="flex flex-col gap-2">
					{/* Hour ruler */}
					<div className="hidden pl-28 sm:block">
						<div className="relative h-5">
							{[6, 9, 12, 15, 18, 21].map((hour) => (
								<span
									key={hour}
									className="absolute -translate-x-1/2 text-xs tabular-nums text-ink-subtle"
									style={{ left: `${((hour * 60 - DAY_START) / span) * 100}%` }}
								>
									{String(hour).padStart(2, "0")}
								</span>
							))}
						</div>
					</div>

					{employee.workingHours.map((d) => {
						const off = d.start === null || d.end === null;
						const left = off ? 0 : ((toMinutes(d.start!) - DAY_START) / span) * 100;
						const width = off
							? 0
							: ((toMinutes(d.end!) - toMinutes(d.start!)) / span) * 100;

						return (
							<div key={d.day} className="flex flex-col gap-1 sm:flex-row sm:items-center sm:gap-0">
								<span
									className={cn(
										"text-sm sm:w-28 sm:shrink-0",
										off ? "text-ink-subtle" : "font-medium text-ink",
									)}
								>
									{d.day}
								</span>
								<div className="relative h-8 flex-1 overflow-hidden rounded-lg bg-neutral-100 dark:bg-white/5">
									{off ? (
										<span className="absolute inset-0 flex items-center pl-3 text-xs text-ink-subtle">
											Day off
										</span>
									) : (
										<div
											className="absolute inset-y-1 flex items-center rounded-md bg-primary-500/90 px-2 text-xs font-medium text-white"
											style={{ left: `${left}%`, width: `${width}%` }}
										>
											<span className="truncate">
												{d.start}–{d.end}
											</span>
										</div>
									)}
								</div>
							</div>
						);
					})}
				</div>
			</Card>

			<Card title="Time off and exceptions">
				<p className="text-sm text-ink-muted">
					Holidays, sick days and one-off schedule changes need a working-hours table that
					doesn&apos;t exist on the backend yet — there is no model, no endpoint and no
					day-of-week concept. This view shows a recurring weekly pattern only.
				</p>
			</Card>
		</div>
	);
}
