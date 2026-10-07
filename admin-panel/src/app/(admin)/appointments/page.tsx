import type { Metadata } from "next";
import AppointmentListView from "@/features/appointments/AppointmentListView";

export const metadata: Metadata = {
	title: "Appointments · Salon Admin",
	description: "Every booking, filterable by date, status and staff member.",
};

export default function AppointmentsPage() {
	return <AppointmentListView />;
}
