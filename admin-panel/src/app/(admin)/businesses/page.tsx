import type { Metadata } from "next";
import BusinessListView from "@/features/businesses/BusinessListView";

export const metadata: Metadata = {
	title: "Businesses · Salon Admin",
	description: "Every salon on the platform.",
};

export default function BusinessesPage() {
	return <BusinessListView />;
}
