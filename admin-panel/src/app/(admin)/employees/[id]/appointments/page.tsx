import type { Metadata } from "next";
import EmployeeAppointmentsView from "@/features/employees/EmployeeAppointmentsView";

export const metadata: Metadata = { title: "Appointments · Salonat Pro" };

export default function EmployeeAppointmentsPage() {
	return <EmployeeAppointmentsView />;
}
