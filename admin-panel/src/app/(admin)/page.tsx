import type { Metadata } from "next";
import DashboardView from "@/features/dashboard/DashboardView";

export const metadata: Metadata = {
	title: "Dashboard · Salon Admin",
	description: "Overview of your salon and platform activity.",
};

// A switch, not a redirect: one bookmarkable URL, no redirect flash, and the
// sidebar's "Dashboard" item stays active for all three roles.
export default function DashboardPage() {
	return <DashboardView />;
}
