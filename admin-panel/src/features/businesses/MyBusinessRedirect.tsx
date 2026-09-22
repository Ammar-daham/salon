"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { resolveBusinessScope } from "@/lib/auth/scope";
import EmptyState from "@/components/ui/EmptyState";
import PageHeader from "@/components/ui/PageHeader";
import { UserIcon } from "@/icons";

/**
 * Business admins and employees share /businesses/[id] with the superadmin —
 * the page composition is identical and only the available actions differ,
 * and those are driven by permissions rather than by URL. This route just
 * resolves "which salon is mine" and forwards.
 */
export default function MyBusinessRedirect() {
	const { user } = useAuth();
	const router = useRouter();
	const scope = resolveBusinessScope(user);

	useEffect(() => {
		if (scope.kind === "business") {
			router.replace(`/businesses/${scope.businessId}`);
		} else if (scope.kind === "platform") {
			// A superadmin has no single salon of their own.
			router.replace("/businesses");
		}
	}, [scope, router]);

	if (scope.kind === "unresolved") {
		return (
			<>
				<PageHeader title="My salon" />
				<EmptyState
					icon={<UserIcon className="size-6" />}
					title="Your account isn't linked to a salon yet"
					description="Ask a platform administrator to attach your account to a business. Once linked, this page will open your salon directly."
				/>
			</>
		);
	}

	return (
		<div className="flex min-h-[40vh] items-center justify-center">
			<span
				className="size-6 animate-spin rounded-full border-2 border-border-strong border-t-primary-500"
				aria-hidden="true"
			/>
		</div>
	);
}
