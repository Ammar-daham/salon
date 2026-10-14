import type { Metadata } from "next";
import { Compass } from "lucide-react";
import EmptyState from "@/components/ui/EmptyState";

export const metadata: Metadata = { title: "Discover · Salonat" };

export default function DiscoverPage() {
	return (
		<EmptyState
			icon={<Compass className="size-7" />}
			title="Nearby salons arrive next"
			description="Search, filters and a distance-sorted list land in the next pass, once this navigation shell is signed off."
		/>
	);
}
