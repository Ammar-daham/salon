import type { Metadata } from "next";
import EmployeeSettingsView from "@/features/employees/EmployeeSettingsView";

export const metadata: Metadata = { title: "Employee settings · Salon Admin" };

export default function EmployeeSettingsPage() {
	return <EmployeeSettingsView />;
}
