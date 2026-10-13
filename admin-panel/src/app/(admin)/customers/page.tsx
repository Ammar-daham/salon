import type { Metadata } from "next";
import CustomerListView from "@/features/customers/CustomerListView";

export const metadata: Metadata = {
	title: "Customers · Salonat Pro",
	description: "Clients, their visit history and their notes.",
};

export default function CustomersPage() {
	return <CustomerListView />;
}
