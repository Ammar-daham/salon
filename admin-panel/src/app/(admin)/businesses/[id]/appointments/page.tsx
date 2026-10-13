import type { Metadata } from "next";
import BusinessAppointmentsView from "@/features/businesses/BusinessAppointmentsView";

export const metadata: Metadata = { title: "Appointments · Salonat Pro" };

export default function BusinessAppointmentsPage() {
	return <BusinessAppointmentsView />;
}
