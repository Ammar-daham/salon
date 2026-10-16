"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Compass, Calendar, User } from "lucide-react";
import { cn } from "@/lib/utils/cn";

const TABS = [
	{ href: "/discover", label: "Discover", icon: Compass },
	{ href: "/bookings", label: "Bookings", icon: Calendar },
	{ href: "/account", label: "Account", icon: User },
] as const;

/**
 * Fixed-bottom, frosted-glass tab bar — the one signature "Apple-like" chrome
 * detail (backdrop-blur over `--color-surface-translucent`). Icons swap
 * filled/outline on the active tab the way SF Symbols do, simulated here with
 * lucide-react's `fill` prop since it ships outline-only icons by default.
 */
export default function TabBar() {
	const pathname = usePathname();

	return (
		<nav
			aria-label="Primary"
			className="fixed inset-x-0 bottom-0 z-[var(--z-index-tabbar)] mx-auto w-full max-w-[480px] border-t border-border-default bg-surface-translucent pb-[env(safe-area-inset-bottom)] backdrop-blur-xl"
		>
			<ul className="flex items-stretch justify-around">
				{TABS.map(({ href, label, icon: Icon }) => {
					const active = pathname === href || pathname.startsWith(`${href}/`);
					return (
						<li key={href} className="flex-1">
							<Link
								href={href}
								aria-current={active ? "page" : undefined}
								className="flex min-h-11 flex-col items-center justify-center gap-0.5 py-2 transition-colors"
							>
								<Icon
									className={cn("size-6", active ? "text-primary-solid" : "text-ink-subtle")}
									fill={active ? "currentColor" : "none"}
									fillOpacity={active ? 0.15 : undefined}
									strokeWidth={active ? 2 : 1.75}
								/>
								<span
									className={cn(
										"text-caption-1 font-medium",
										active ? "text-primary-solid" : "text-ink-subtle",
									)}
								>
									{label}
								</span>
							</Link>
						</li>
					);
				})}
			</ul>
		</nav>
	);
}
