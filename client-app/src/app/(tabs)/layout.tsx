import React from "react";
import AppShell from "@/components/layout/AppShell";
import TabBar from "@/components/navigation/TabBar";

export default function TabsLayout({ children }: { children: React.ReactNode }) {
	return (
		<AppShell withTabBarOffset>
			<div className="flex flex-1 flex-col">{children}</div>
			<TabBar />
		</AppShell>
	);
}
