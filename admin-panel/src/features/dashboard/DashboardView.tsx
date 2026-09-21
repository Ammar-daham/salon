"use client";

import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/context/AuthContext";
import { queryKeys } from "@/lib/query/queryKeys";
import { businessesRepository } from "@/lib/resources/businesses/businesses.api";
import { usersRepository } from "@/lib/resources/users/users.api";
import { resolveBusinessScope } from "@/lib/auth/scope";
import PageHeader from "@/components/ui/PageHeader";
import EmptyState from "@/components/ui/EmptyState";
import StatTile from "@/components/ui/StatTile";
import Card from "@/components/ui/Card";
import { BoxIcon, GroupIcon, PieChartIcon, UserCircleIcon, UserIcon } from "@/icons";

function greeting(): string {
	const h = new Date().getHours();
	if (h < 12) return "Good morning";
	if (h < 18) return "Good afternoon";
	return "Good evening";
}

/**
 * Platform numbers derived from the two list endpoints that genuinely exist.
 * One GET /business and one GET /users feed every tile — both are N+1 on the
 * server, so they are fetched once into the shared cache and every tile reads
 * from those arrays rather than firing its own request.
 */
function SuperAdminTiles() {
	const businesses = useQuery({
		queryKey: queryKeys.businesses.list(),
		queryFn: ({ signal }) => businessesRepository.list({ signal }),
	});
	const users = useQuery({
		queryKey: queryKeys.users.list(),
		queryFn: ({ signal }) => usersRepository.list({ signal }),
	});

	const all = businesses.data ?? [];
	const people = users.data ?? [];

	return (
		<div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4 md:gap-6">
			<StatTile
				icon={<BoxIcon className="size-5" />}
				label="Total businesses"
				value={all.length}
				loading={businesses.isPending}
			/>
			<StatTile
				icon={<PieChartIcon className="size-5" />}
				label="Active businesses"
				value={all.filter((b) => b.status === "APPROVED").length}
				hint="Approved and operating"
				loading={businesses.isPending}
			/>
			<StatTile
				icon={<GroupIcon className="size-5" />}
				label="Total users"
				value={people.length}
				loading={users.isPending}
			/>
			<StatTile
				icon={<UserCircleIcon className="size-5" />}
				label="Employees"
				value={people.filter((u) => u.role === "EMPLOYEE").length}
				loading={users.isPending}
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
