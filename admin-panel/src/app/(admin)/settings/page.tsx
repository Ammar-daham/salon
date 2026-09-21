import type { Metadata } from "next";
import PagePlaceholder from "@/components/ui/PagePlaceholder";

export const metadata: Metadata = {
	title: "Settings · Salon Admin",
};

export default function SettingsPage() {
	return (
		<PagePlaceholder
			title="Settings"
			description="Your profile, your salon and platform defaults."
			phase="Phase 6"
			detail="Profile, salon and platform settings arrive in the final feature phase."
		/>
	);
}
