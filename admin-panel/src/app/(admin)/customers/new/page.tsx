import type { Metadata } from "next";
import CustomerCreateView from "@/features/customers/CustomerCreateView";

export const metadata: Metadata = { title: "Add client · Salon Admin" };

export default function NewCustomerPage() {
	return <CustomerCreateView />;
}
