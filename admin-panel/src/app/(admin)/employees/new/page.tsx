import type { Metadata } from "next";
import UserCreateView from "@/features/users/UserCreateView";

export const metadata: Metadata = { title: "Add employee · Salon Admin" };

// Replaces the old /signup route. Restricted to staff roles — a salon admin
// adding team members has no business creating platform administrators.
export default function NewEmployeePage() {
	return (
		<UserCreateView
			title="Add employee"
			description="Create a staff account for your salon."
			allowedRoles={["EMPLOYEE", "ADMIN"]}
			returnTo="/employees"
		/>
	);
}
