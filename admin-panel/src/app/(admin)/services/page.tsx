import type { Metadata } from "next";
import ServicesView from "@/features/services/ServicesView";

export const metadata: Metadata = {
	title: "Services · Salon Admin",
	description: "Treatments, duration and pricing.",
};

export default function ServicesPage() {
	return <ServicesView />;
}
