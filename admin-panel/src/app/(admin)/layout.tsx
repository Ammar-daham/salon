"use client";

import { useAuth } from "@/context/AuthContext";
import { useSidebar } from "@/context/SidebarContext";
import RouteGuard from "@/components/auth/RouteGuard";
import AppHeader from "@/layout/AppHeader";
import AppSidebar from "@/layout/AppSidebar";
import Backdrop from "@/layout/Backdrop";
import { useRouter } from "next/navigation";
import React, { useEffect } from "react";

export default function AdminLayout({
	children,
}: {
	children: React.ReactNode;
}) {
	const { isExpanded, isHovered, isMobileOpen } = useSidebar();
	const { user, isLoading } = useAuth();
	const router = useRouter();

	useEffect(() => {
		if (!isLoading && !user) {
			router.replace("/signin");
		}
	}, [isLoading, user, router]);

	// Mirrors the sidebar's own widths. Both pivot at `lg`.
	const mainContentMargin = isMobileOpen
		? "ml-0"
		: isExpanded || isHovered
		? "lg:ml-[290px]"
		: "lg:ml-[90px]";

	if (isLoading || !user) {
		return (
			<div className="flex min-h-screen items-center justify-center bg-surface">
				<div className="flex flex-col items-center gap-3">
					<span
						className="size-6 animate-spin rounded-full border-2 border-border-strong border-t-primary-500"
						aria-hidden="true"
					/>
					<p className="text-sm text-ink-muted">Loading your workspace…</p>
				</div>
			</div>
		);
	}

	return (
		<div className="min-h-screen bg-surface xl:flex">
			<AppSidebar />
			<Backdrop />
			<div className={`flex-1 transition-all duration-300 ease-in-out ${mainContentMargin}`}>
				<AppHeader />
				<div className="mx-auto max-w-(--breakpoint-2xl) p-4 md:p-6">
					<RouteGuard>{children}</RouteGuard>
				</div>
			</div>
		</div>
	);
}
