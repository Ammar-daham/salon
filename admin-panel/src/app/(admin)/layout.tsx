"use client";

import { useAuth } from "@/context/AuthContext";
import { useSidebar } from "@/context/SidebarContext";
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

	// Dynamic class for main content margin based on sidebar state
	const mainContentMargin = isMobileOpen
		? "ml-0"
		: isExpanded || isHovered
		? "lg:ml-[290px]"
		: "lg:ml-[90px]";

	if (isLoading || !user) {
		return (
			<div className="flex items-center justify-center min-h-screen">
				<p className="text-gray-500 dark:text-gray-400">Loading…</p>
			</div>
		);
	}

	return (
		<div className="min-h-screen xl:flex">
			{/* Sidebar and Backdrop */}
			<AppSidebar />
			<Backdrop />
			{/* Main Content Area */}
			<div
				className={`flex-1 transition-all  duration-300 ease-in-out ${mainContentMargin}`}
			>
				{/* Header */}
				<AppHeader />
				{/* Page Content */}
				<div className="p-4 mx-auto max-w-(--breakpoint-2xl) md:p-6">{children}</div>
			</div>
		</div>
	);
}
