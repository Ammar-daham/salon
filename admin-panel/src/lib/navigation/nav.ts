import type { ComponentType, SVGProps } from "react";
import {
	BellIcon,
	BoltIcon,
	BoxIcon,
	CalenderIcon,
	DollarLineIcon,
	GridIcon,
	GroupIcon,
	ListIcon,
	PieChartIcon,
	TaskIcon,
	UserCircleIcon,
	UserIcon,
} from "@/icons";
import type { Permission } from "@/lib/auth/permissions";

export interface NavItem {
	id: string;
	label: string;
	href: string;
	/**
	 * A component, not an element. The old nav stored `<GridIcon />` at module
	 * scope, which instantiated React elements at import time and made per-item
	 * sizing and colouring impossible.
	 */
	icon: ComponentType<SVGProps<SVGSVGElement>>;
	permission?: Permission;
	/** "exact" only for "/". Everything else highlights on nested routes too. */
	match?: "exact" | "prefix";
	/** Hidden for platform-scoped users (SUPER_ADMIN has no single salon). */
	requiresBusinessScope?: boolean;
}

export interface NavSection {
	id: string;
	label: string | null;
	items: NavItem[];
}

export const NAV_SECTIONS: NavSection[] = [
	{
		id: "overview",
		label: null,
		items: [
			{
				id: "dashboard",
				label: "Dashboard",
				href: "/",
				icon: GridIcon,
				match: "exact",
				permission: "dashboard:view",
			},
		],
	},
	{
		id: "scheduling",
		label: "Scheduling",
		items: [
			{
				id: "appointments",
				label: "Appointments",
				href: "/appointments",
				icon: TaskIcon,
				permission: "appointment:list",
			},
			{
				id: "calendar",
				label: "Calendar",
				href: "/calendar",
				icon: CalenderIcon,
				permission: "calendar:view",
			},
		],
	},
	{
		id: "salon",
		label: "Salon",
		items: [
			{
				id: "customers",
				label: "Customers",
				href: "/customers",
				icon: UserIcon,
				permission: "customer:list",
			},
			{
				id: "services",
				label: "Services",
				href: "/services",
				icon: ListIcon,
				permission: "service:list",
			},
			{
				id: "employees",
				label: "Employees",
				href: "/employees",
				icon: UserCircleIcon,
				permission: "employee:list",
			},
			{
				id: "my-business",
				label: "My salon",
				href: "/my-business",
				icon: BoxIcon,
				permission: "business:view",
				requiresBusinessScope: true,
			},
		],
	},
	{
		id: "platform",
		label: "Platform",
		items: [
			{
				id: "businesses",
				label: "Businesses",
				href: "/businesses",
				icon: BoxIcon,
				permission: "business:list",
			},
			{
				id: "users",
				label: "Users",
				href: "/users",
				icon: GroupIcon,
				permission: "user:list",
			},
			{
				id: "subscriptions",
				label: "Subscriptions",
				href: "/subscriptions",
				icon: DollarLineIcon,
				permission: "subscription:manage",
			},
			{
				id: "billing",
				label: "Billing",
				href: "/subscriptions/billing",
				icon: DollarLineIcon,
				permission: "subscription:billing",
			},
		],
	},
	{
		id: "insights",
		label: "Insights",
		items: [
			{
				id: "reports",
				label: "Reports",
				href: "/reports",
				icon: PieChartIcon,
				permission: "report:view",
			},
		],
	},
	{
		id: "system",
		label: "System",
		items: [
			{
				id: "notifications",
				label: "Notifications",
				href: "/notifications",
				icon: BellIcon,
				permission: "notification:view",
			},
			{
				id: "settings",
				label: "Settings",
				href: "/settings",
				icon: BoltIcon,
				permission: "settings:profile",
			},
		],
	},
];
