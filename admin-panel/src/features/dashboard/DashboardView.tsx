"use client";

import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { useBusinessPage } from "@/lib/resources/businesses/businesses.hooks";
import { useUserPage } from "@/lib/resources/users/users.hooks";
import { resolveBusinessScope } from "@/lib/auth/scope";
import { businessListHref } from "@/lib/resources/businesses/businesses.types";
import PageHeader from "@/components/ui/PageHeader";
import EmptyState from "@/components/ui/EmptyState";
import StatTile from "@/components/ui/StatTile";
import Card from "@/components/ui/Card";
import { BoxIcon, GroupIcon, PieChartIcon, TimeIcon, UserCircleIcon, UserIcon } from "@/icons";

function greeting(): string {
	const h = new Date().getHours();
	if (h < 12) return "Good morning";
	if (h < 18) return "Good afternoon";
	return "Good evening";
}

/**
 * Platform numbers, each the total of a one-row page (BE-15): the server counts, so the dashboard
 * downloads five rows rather than every salon and user on the platform.
 */
function SuperAdminTiles() {
	const businesses = useBusinessPage({ size: 1 });
	const pending = useBusinessPage({ size: 1, status: "PENDING" });
	const approved = useBusinessPage({ size: 1, status: "APPROVED" });
	const users = useUserPage({ size: 1 });
	const employees = useUserPage({ size: 1, role: "EMPLOYEE" });

	return (
		<div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 md:gap-6">
			<Link
				href={businessListHref("PENDING")}
				className="block rounded-card transition-shadow hover:shadow-sm focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-500"
			>
				<StatTile
					icon={<TimeIcon className="size-5" />}
					label="Pending registrations"
					value={pending.data?.totalItems ?? 0}
					hint="Waiting for your review"
					loading={pending.isPending}
					className="h-full"
				/>
			</Link>
			<StatTile
				icon={<BoxIcon className="size-5" />}
				label="Total businesses"
				value={businesses.data?.totalItems ?? 0}
				loading={businesses.isPending}
			/>
			<StatTile
				icon={<PieChartIcon className="size-5" />}
				label="Active businesses"
				value={approved.data?.totalItems ?? 0}
				hint="Approved and operating"
				loading={approved.isPending}
			/>
			<StatTile
				icon={<GroupIcon className="size-5" />}
				label="Total users"
				value={users.data?.totalItems ?? 0}
				loading={users.isPending}
			/>
			<StatTile
				icon={<UserCircleIcon className="size-5" />}
				label="Employees"
				value={employees.data?.totalItems ?? 0}
				loading={employees.isPending}
			/>
		</div>
	);
}

export default function DashboardView() {
	const { user } = useAuth();
	const scope = resolveBusinessScope(user);

	const isSuperAdmin = user?.role === "SUPER_ADMIN";

	return (
		<>
			<PageHeader
				title={`${greeting()}, ${user?.firstName ?? "there"}`}
				description={
					isSuperAdmin
						? "Platform activity across every salon."
						: "What's happening at your salon today."
				}
			/>

			{isSuperAdmin ? (
				<SuperAdminTiles />
			) : scope.kind === "unresolved" ? (
				<EmptyState
					icon={<UserIcon className="size-6" />}
					title="Your account isn't linked to a salon yet"
					description="Ask a platform administrator to attach your account to a business. Once linked, your salon's bookings, team and clients will appear here."
				/>
			) : (
				<Card
					title="Your salon at a glance"
					description="Today's bookings, revenue and team availability."
				>
					<p className="text-sm text-ink-muted">
						These figures depend on the appointments feature, which arrives in Phase 4.
					</p>
				</Card>
			)}

			<Card
				className="mt-4 md:mt-6"
				title="What's next"
				description="The panel is being rebuilt feature by feature."
			>
				<ul className="space-y-2 text-sm text-ink-muted">
					<li>
						<span className="font-medium text-ink">Phase 1</span> — Businesses, end to
						end, on live data.
					</li>
					<li>
						<span className="font-medium text-ink">Phase 2</span> — Services and users.
					</li>
					<li>
						<span className="font-medium text-ink">Phase 3</span> — Employees and
						customers.
					</li>
					<li>
						<span className="font-medium text-ink">Phase 4</span> — Appointments and the
						calendar.
					</li>
				</ul>
			</Card>
		</>
	);
}
