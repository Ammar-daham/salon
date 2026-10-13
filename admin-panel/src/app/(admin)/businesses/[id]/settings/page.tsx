import type { Metadata } from "next";
import BusinessSettingsView from "@/features/businesses/BusinessSettingsView";

export const metadata: Metadata = { title: "Salon settings · Salonat Pro" };

export default function BusinessSettingsPage() {
	return <BusinessSettingsView />;
}
