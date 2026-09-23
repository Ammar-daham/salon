import type { Metadata } from "next";
import CustomerListView from "@/features/customers/CustomerListView";

export const metadata: Metadata = {
	title: "Customers · Salon Admin",
	description: "Clients, their visit history and their notes.",
};

export default function CustomersPage() {
	return <CustomerListView />;
}
