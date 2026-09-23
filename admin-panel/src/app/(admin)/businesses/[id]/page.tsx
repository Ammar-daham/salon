import type { Metadata } from "next";
import BusinessOverviewView from "@/features/businesses/BusinessOverviewView";

export const metadata: Metadata = {
	title: "Salon overview · Salon Admin",
};

export default function BusinessOverviewPage() {
	return <BusinessOverviewView />;
}
