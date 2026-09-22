import type { Metadata } from "next";
import EmployeesLanding from "@/features/users/EmployeesLanding";

export const metadata: Metadata = {
	title: "Employees · Salon Admin",
	description: "Staff, their schedules and their availability.",
};

export default function EmployeesPage() {
	return <EmployeesLanding />;
}
