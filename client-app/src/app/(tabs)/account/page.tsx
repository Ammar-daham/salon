import type { Metadata } from "next";
import { User } from "lucide-react";
import EmptyState from "@/components/ui/EmptyState";

export const metadata: Metadata = { title: "Account · Salonat" };

export default function AccountPage() {
	return (
		<EmptyState
			icon={<User className="size-7" />}
			title="Sign in to manage your account"
			description="Profile details, theme and sign-out land here alongside the sign-in flow."
		/>
	);
}
