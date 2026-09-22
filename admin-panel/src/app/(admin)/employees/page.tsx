import type { Metadata } from "next";
import EmployeeListView from "@/features/employees/EmployeeListView";

export const metadata: Metadata = {
	title: "Employees · Salon Admin",
	description: "Staff, their titles and their schedules.",
};

export default function EmployeesPage() {
	return <EmployeeListView />;
}
