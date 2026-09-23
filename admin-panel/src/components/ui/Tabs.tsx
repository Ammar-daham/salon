"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils/cn";
import { isNavActive } from "@/lib/navigation/isNavActive";

export interface TabItem {
	label: string;
	href: string;
	/** Exact match for the tab that owns the parent route itself. */
	exact?: boolean;
}

/** Route-backed tabs: each tab is a real URL, so a detail view's sub-page is
 *  linkable, refreshable and back-button friendly. */
export default function Tabs({ items, className }: { items: TabItem[]; className?: string }) {
	const pathname = usePathname();

	return (
		<div className={cn("border-b border-border-default", className)}>
			<nav className="-mb-px flex gap-1 overflow-x-auto" aria-label="Sections">
				{items.map((item) => {
					const active = isNavActive(pathname, item.href, item.exact ? "exact" : "prefix");
					return (
						<Link
							key={item.href}
							href={item.href}
							aria-current={active ? "page" : undefined}
							className={cn(
								"whitespace-nowrap border-b-2 px-4 py-3 text-sm font-medium transition-colors",
								active
									? "border-primary-500 text-primary-700 dark:text-primary-300"
									: "border-transparent text-ink-muted hover:border-border-strong hover:text-ink",
							)}
						>
							{item.label}
						</Link>
					);
				})}
			</nav>
		</div>
	);
}
