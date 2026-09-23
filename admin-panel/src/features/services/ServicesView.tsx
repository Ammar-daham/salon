"use client";

import { useAuth } from "@/context/AuthContext";
import { resolveBusinessScope } from "@/lib/auth/scope";
import PageHeader from "@/components/ui/PageHeader";
import EmptyState from "@/components/ui/EmptyState";
import { UserIcon } from "@/icons";
import ServicesManager from "./ServicesManager";

export default function ServicesView() {
	const { user } = useAuth();
	const scope = resolveBusinessScope(user);

	if (scope.kind === "unresolved") {
		return (
			<>
				<PageHeader title="Services" />
				<EmptyState
					icon={<UserIcon className="size-6" />}
					title="Your account isn't linked to a salon yet"
					description="Ask a platform administrator to attach your account to a business. Its service menu will appear here once linked."
				/>
			</>
		);
	}

	const platformWide = scope.kind === "platform";

	return (
		<>
			<PageHeader
				title="Services"
				description={
					platformWide
						? "Every treatment offered across the platform."
						: "The treatments your salon offers, with duration and pricing."
				}
			/>
			<ServicesManager businessId={platformWide ? null : scope.businessId} />
		</>
	);
}
