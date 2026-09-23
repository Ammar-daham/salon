"use client";

import Link from "next/link";
import { useEmployee } from "@/lib/resources/employees/employees.hooks";
import PageHeader from "@/components/ui/PageHeader";
import EmptyState from "@/components/ui/EmptyState";
import Tabs from "@/components/ui/Tabs";
import StatusBadge from "@/components/ui/StatusBadge";
import InitialsAvatar from "@/components/ui/InitialsAvatar";
import { Skeleton } from "@/components/ui/Skeleton";
import { ChevronLeftIcon, UserCircleIcon } from "@/icons";

export default function EmployeeDetailShell({
	id,
	children,
}: {
	id: number;
	children: React.ReactNode;
}) {
	const { data: employee, isPending, source } = useEmployee(id);

	if (isPending) {
		return (
			<div className="flex flex-col gap-4">
				<Skeleton className="h-6 w-32" />
				<Skeleton className="h-16 w-full max-w-md" />
				<Skeleton className="h-64 rounded-card" />
			</div>
		);
	}

	if (!employee) {
		return (
			<EmptyState
				icon={<UserCircleIcon className="size-6" />}
				title="Employee not found"
				description="This person may have been removed from the team."
			/>
		);
	}

	const tabs = [
		{ label: "Profile", href: `/employees/${id}`, exact: true },
		{ label: "Schedule", href: `/employees/${id}/schedule` },
		{ label: "Appointments", href: `/employees/${id}/appointments` },
	];

	return (
		<>
			<Link
				href="/employees"
				className="mb-4 inline-flex items-center gap-1 text-sm text-ink-muted transition-colors hover:text-ink"
			>
				<ChevronLeftIcon className="size-4" />
				All employees
			</Link>

			<div className="mb-6 flex items-center gap-4">
				<InitialsAvatar firstName={employee.firstName} lastName={employee.lastName} size="lg" />
				<PageHeader
					className="mb-0"
					title={`${employee.firstName} ${employee.lastName}`}
					description={`${employee.title} · ${employee.businessName}`}
					sampleData={source === "mock"}
				/>
			</div>

			<div className="mb-4 flex flex-wrap items-center gap-3">
				<StatusBadge tone={employee.isActive ? "success" : "neutral"}>
					{employee.isActive ? "Active" : "Inactive"}
				</StatusBadge>
				<span className="text-sm text-ink-subtle">
					Joined{" "}
					{new Date(employee.hiredAt).toLocaleDateString(undefined, {
						year: "numeric",
						month: "long",
					})}
				</span>
			</div>

			<Tabs items={tabs} className="mb-6" />
			{children}
		</>
	);
}
