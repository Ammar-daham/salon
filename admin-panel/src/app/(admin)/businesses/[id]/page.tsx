import type { Metadata } from "next";
import BusinessOverviewView from "@/features/businesses/BusinessOverviewView";

export const metadata: Metadata = {
	title: "Salon overview · Salonat Pro",
};

export default function BusinessOverviewPage() {
	return <BusinessOverviewView />;
}
