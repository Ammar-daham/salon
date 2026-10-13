import type { Metadata } from "next";
import EmployeeProfileView from "@/features/employees/EmployeeProfileView";

export const metadata: Metadata = { title: "Employee · Salonat Pro" };

export default function EmployeeProfilePage() {
	return <EmployeeProfileView />;
}
