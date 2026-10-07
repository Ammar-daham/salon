import type { Metadata } from "next";
import BusinessHoursView from "@/features/businesses/BusinessHoursView";

export const metadata: Metadata = { title: "Opening hours · Salon Admin" };

export default function BusinessHoursPage() {
	return <BusinessHoursView />;
}
