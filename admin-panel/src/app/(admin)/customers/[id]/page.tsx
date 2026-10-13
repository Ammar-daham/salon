import type { Metadata } from "next";
import CustomerProfileView from "@/features/customers/CustomerProfileView";

export const metadata: Metadata = { title: "Client · Salonat Pro" };

export default function CustomerProfilePage() {
	return <CustomerProfileView />;
}
