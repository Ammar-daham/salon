import type { Metadata } from "next";
import BusinessHoursView from "@/features/businesses/BusinessHoursView";

export const metadata: Metadata = { title: "Opening hours · Salonat Pro" };

export default function BusinessHoursPage() {
	return <BusinessHoursView />;
}
