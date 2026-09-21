import type { Metadata } from "next";
import PagePlaceholder from "@/components/ui/PagePlaceholder";

export const metadata: Metadata = {
	title: "My salon · Salon Admin",
};

export default function MyBusinessPage() {
	return (
		<PagePlaceholder
			title="My salon"
			description="Your salon's profile, services and team."
			phase="Phase 1"
			detail="This will redirect straight to your own salon's detail page once the businesses feature ships."
		/>
	);
}
