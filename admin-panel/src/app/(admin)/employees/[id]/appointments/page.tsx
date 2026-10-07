import type { Metadata } from "next";
import EmployeeAppointmentsView from "@/features/employees/EmployeeAppointmentsView";

export const metadata: Metadata = { title: "Appointments · Salon Admin" };

export default function EmployeeAppointmentsPage() {
	return <EmployeeAppointmentsView />;
}
