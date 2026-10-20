import type { Metadata } from "next";
import { Calendar } from "lucide-react";
import EmptyState from "@/components/ui/EmptyState";

export const metadata: Metadata = { title: "Bookings · Salonat" };

export default function BookingsPage() {
	return (
		<EmptyState
			icon={<Calendar className="size-7" />}
			title="Your bookings will live here"
			description="Upcoming and past appointments arrive once the booking flow is built."
		/>
	);
}
