import type { Metadata } from "next";
import EmployeeSettingsView from "@/features/employees/EmployeeSettingsView";

export const metadata: Metadata = { title: "Employee settings · Salonat Pro" };

export default function EmployeeSettingsPage() {
	return <EmployeeSettingsView />;
}
