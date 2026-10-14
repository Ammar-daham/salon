import React from "react";
import { cn } from "@/lib/utils/cn";

/**
 * Centers the app in a phone-width column (480px) on any viewport, with a
 * subtle neutral fill outside it on wide screens — this is a mobile app, not a
 * responsive website, so it reads as one even inside a desktop browser rather
 * than stretching full-bleed. `contentClassName` lets a screen opt out of the
 * default padding (e.g. a page with its own full-bleed header image).
 */
export default function AppShell({
	children,
	className,
	withTabBarOffset = false,
}: {
	children: React.ReactNode;
	className?: string;
	/** Adds bottom padding so content isn't hidden behind the fixed tab bar. */
	withTabBarOffset?: boolean;
}) {
	return (
		<div className="min-h-dvh bg-neutral-100 dark:bg-black">
			<div
				className={cn(
					"relative mx-auto flex min-h-dvh w-full max-w-[480px] flex-col bg-surface pt-[env(safe-area-inset-top)]",
					withTabBarOffset && "pb-[calc(env(safe-area-inset-bottom)+72px)]",
					className,
				)}
			>
				{children}
			</div>
		</div>
	);
}
