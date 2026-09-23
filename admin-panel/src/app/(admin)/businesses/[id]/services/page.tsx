import type { Metadata } from "next";
import BusinessServicesView from "@/features/businesses/BusinessServicesView";

export const metadata: Metadata = { title: "Services · Salon Admin" };

export default function BusinessServicesPage() {
	return <BusinessServicesView />;
}
