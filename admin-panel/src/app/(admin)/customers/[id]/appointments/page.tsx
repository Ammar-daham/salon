import type { Metadata } from "next";
import CustomerAppointmentsView from "@/features/customers/CustomerAppointmentsView";

export const metadata: Metadata = { title: "Visit history · Salon Admin" };

export default function CustomerAppointmentsPage() {
	return <CustomerAppointmentsView />;
}
