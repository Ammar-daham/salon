import type { Metadata } from "next";
import AppointmentListView from "@/features/appointments/AppointmentListView";

export const metadata: Metadata = {
	title: "Appointments · Salonat Pro",
	description: "Every booking, filterable by date, status and staff member.",
};

export default function AppointmentsPage() {
	return <AppointmentListView />;
}
