import type { Metadata } from "next";
import CustomerNotesView from "@/features/customers/CustomerNotesView";

export const metadata: Metadata = { title: "Notes · Salon Admin" };

export default function CustomerNotesPage() {
	return <CustomerNotesView />;
}
