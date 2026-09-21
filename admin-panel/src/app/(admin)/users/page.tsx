import type { Metadata } from "next";
import PagePlaceholder from "@/components/ui/PagePlaceholder";

export const metadata: Metadata = {
	title: "Users · Salon Admin",
};

export default function UsersPage() {
	return (
		<PagePlaceholder
			title="Users"
			description="Every account on the platform, across all salons."
			phase="Phase 2"
			detail="The users table and account editor arrive with the live user CRUD work."
		/>
	);
}
