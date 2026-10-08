"use client";

import { useAuth } from "@/context/AuthContext";
import { ROLE_LABELS } from "@/lib/resources/auth/auth.types";
import PageHeader from "@/components/ui/PageHeader";
import Card from "@/components/ui/Card";
import ChangePasswordForm from "./ChangePasswordForm";

/** The signed-in user's own profile and password. A salon's settings are on its own page. */
export default function SettingsView() {
	const { user } = useAuth();
	// The admin layout renders no page until someone is signed in.
	if (!user) return null;

	const details = [
		["Name", `${user.firstName} ${user.lastName}`],
		["Email", user.email],
		["Role", ROLE_LABELS[user.role]],
	];

	return (
		<>
			<PageHeader title="Settings" description="Your profile and password." />
			<div className="flex max-w-3xl flex-col gap-5 md:gap-6">
				<Card title="Profile">
					<dl className="flex flex-col gap-3 text-sm">
						{details.map(([label, value]) => (
							<div key={label} className="flex justify-between gap-4">
								<dt className="text-ink-muted">{label}</dt>
								<dd className="min-w-0 truncate text-ink">{value}</dd>
							</div>
						))}
					</dl>
				</Card>
				<ChangePasswordForm email={user.email} />
			</div>
		</>
	);
}
