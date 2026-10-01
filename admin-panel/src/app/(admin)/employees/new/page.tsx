import type { Metadata } from "next";
import EmployeeCreateView from "@/features/employees/EmployeeCreateView";

export const metadata: Metadata = { title: "Add employee · Salon Admin" };

// Replaces the old /signup route. Restricted to staff roles — a salon admin
// adding team members has no business creating platform administrators.
//
// Unlike UserCreateView (used by /users/new for any role), this also writes the
// staff roster entry that backs the Employees list - see EmployeeCreateView.
export default function NewEmployeePage() {
	return <EmployeeCreateView />;
}
