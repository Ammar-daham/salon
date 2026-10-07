import type { Metadata } from "next";
import BusinessAppointmentsView from "@/features/businesses/BusinessAppointmentsView";

export const metadata: Metadata = { title: "Appointments · Salon Admin" };

export default function BusinessAppointmentsPage() {
	return <BusinessAppointmentsView />;
}
