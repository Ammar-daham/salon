import type { Metadata } from "next";
import CustomerEditView from "@/features/customers/CustomerEditView";

export const metadata: Metadata = { title: "Edit client · Salonat Pro" };

export default function EditCustomerPage() {
	return <CustomerEditView />;
}
