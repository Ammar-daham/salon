import type { Metadata } from "next";
import PagePlaceholder from "@/components/ui/PagePlaceholder";

export const metadata: Metadata = {
	title: "Businesses · Salon Admin",
};

export default function BusinessesPage() {
	return (
		<PagePlaceholder
			title="Businesses"
			description="Every salon on the platform — create, review and manage them."
			phase="Phase 1"
			detail="The businesses table, create wizard and detail tabs land next. This is the first feature wired to live data."
		/>
	);
}
