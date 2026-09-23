"use client";

import { usePathname } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { canAccessRoute } from "@/lib/auth/routeAccess";
import { LockIcon } from "@/icons";

function Forbidden() {
	return (
		<div className="flex min-h-[60vh] flex-col items-center justify-center px-6 text-center">
			<span className="flex size-14 items-center justify-center rounded-full bg-neutral-100 text-neutral-500 dark:bg-white/5 dark:text-neutral-400">
				<LockIcon className="size-6" />
			</span>
			<h1 className="mt-5 text-h1 font-semibold text-ink">Not available for your role</h1>
			<p className="mt-2 max-w-md text-sm text-ink-muted">
				You don&apos;t have access to this page. If you think that&apos;s wrong, ask a
				platform administrator to review your permissions.
			</p>
			<Link
				href="/"
				className="mt-6 inline-flex items-center justify-center rounded-lg bg-primary-solid px-5 py-3 text-sm font-medium text-white transition-colors hover:bg-primary-700"
			>
				Back to dashboard
			</Link>
		</div>
	);
}

/**
 * Renders the forbidden state in place rather than redirecting: a redirect
 * would lose the URL the user tried, and bounce them somewhere they didn't ask
 * for. Reads the same permission module the sidebar does, so a hidden nav item
 * and a blocked route can never disagree.
 */
export default function RouteGuard({ children }: { children: React.ReactNode }) {
	const { user } = useAuth();
	const pathname = usePathname();

	if (!canAccessRoute(user, pathname)) return <Forbidden />;
	return <>{children}</>;
}
