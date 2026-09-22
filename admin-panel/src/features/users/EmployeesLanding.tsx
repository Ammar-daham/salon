"use client";

import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { can } from "@/lib/auth/permissions";
import PageHeader from "@/components/ui/PageHeader";
import EmptyState from "@/components/ui/EmptyState";
import Button from "@/components/ui/button/Button";
import { PlusIcon, UserCircleIcon } from "@/icons";

/**
 * Creating staff already works — POST /users is correctly scoped server-side.
 * The *roster* does not: GET /users is platform-wide and unscopeable, so an
 * admin's list would show other salons' staff. Until the backend can scope it,
 * this page offers the action that genuinely works and is honest about the rest.
 */
export default function EmployeesLanding() {
	const { user } = useAuth();
	const router = useRouter();
	const canCreate = can(user, "employee:create");

	return (
		<>
			<PageHeader
				title="Employees"
				description="Staff accounts for your salon."
				actions={
					canCreate && (
						<Button
							startIcon={<PlusIcon className="size-4" />}
							onClick={() => router.push("/employees/new")}
						>
							Add employee
						</Button>
					)
				}
			/>
			<EmptyState
				icon={<UserCircleIcon className="size-6" />}
				title="The staff roster arrives in Phase 3"
				description="You can already create staff accounts — that endpoint is scoped to your salon correctly. Listing them isn't possible yet: the users endpoint returns every account on the platform with no way to filter by salon."
				action={
					canCreate && (
						<Button
							variant="outline"
							startIcon={<PlusIcon className="size-4" />}
							onClick={() => router.push("/employees/new")}
						>
							Add employee
						</Button>
					)
				}
			/>
		</>
	);
}
