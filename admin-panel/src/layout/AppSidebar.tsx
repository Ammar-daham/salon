"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useSidebar } from "@/context/SidebarContext";
import { HorizontaLDots } from "@/icons";
import { isNavActive } from "@/lib/navigation/isNavActive";
import { useVisibleNav } from "@/lib/navigation/useVisibleNav";
import { cn } from "@/lib/utils/cn";

function Wordmark({ compact }: { compact: boolean }) {
	return (
		<Link href="/" className="flex items-center gap-3" aria-label="Salon — dashboard">
			<span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary-solid font-semibold text-white">
				S
			</span>
			{!compact && (
				<span className="flex flex-col leading-tight">
					<span className="text-base font-semibold tracking-tight text-ink">Salon</span>
					<span className="text-xs text-ink-subtle">Beauty &amp; wellness</span>
				</span>
			)}
		</Link>
	);
}

const AppSidebar: React.FC = () => {
	const { isExpanded, isMobileOpen, isHovered, setIsHovered } = useSidebar();
	const pathname = usePathname();
	const sections = useVisibleNav();

	// Labels are hidden only when the rail is collapsed AND not being hovered.
	const showLabels = isExpanded || isHovered || isMobileOpen;

	return (
		<aside
			className={cn(
				"fixed left-0 top-0 z-40 mt-16 flex h-screen flex-col border-r border-border-default bg-surface-raised px-4 transition-all duration-300 ease-in-out lg:mt-0",
				showLabels ? "w-[290px]" : "w-[90px]",
				isMobileOpen ? "translate-x-0" : "-translate-x-full",
				"lg:translate-x-0",
			)}
			onMouseEnter={() => !isExpanded && setIsHovered(true)}
			onMouseLeave={() => setIsHovered(false)}
		>
			<div className={cn("flex py-7", showLabels ? "justify-start px-1" : "lg:justify-center")}>
				<Wordmark compact={!showLabels} />
			</div>

			<div className="flex flex-col overflow-y-auto pb-6 duration-300 ease-linear no-scrollbar">
				<nav className="flex flex-col gap-6">
					{sections.map((section) => (
						<div key={section.id}>
							{section.label ? (
								<h2
									className={cn(
										"mb-2 flex px-3 text-xs font-medium uppercase leading-5 tracking-wide text-ink-subtle",
										showLabels ? "justify-start" : "lg:justify-center",
									)}
								>
									{showLabels ? section.label : <HorizontaLDots />}
								</h2>
							) : null}

							<ul className="flex flex-col gap-1">
								{section.items.map((item) => {
									const active = isNavActive(pathname, item.href, item.match);
									const Icon = item.icon;
									return (
										<li key={item.id}>
											<Link
												href={item.href}
												aria-current={active ? "page" : undefined}
												title={showLabels ? undefined : item.label}
												className={cn(
													"menu-item group",
													active ? "menu-item-active" : "menu-item-inactive",
													!showLabels && "lg:justify-center",
												)}
											>
												<span
													className={
														active ? "menu-item-icon-active" : "menu-item-icon-inactive"
													}
												>
													<Icon className="size-5" />
												</span>
												{showLabels && <span className="truncate">{item.label}</span>}
											</Link>
										</li>
									);
								})}
							</ul>
						</div>
					))}
				</nav>
			</div>
		</aside>
	);
};

export default AppSidebar;
