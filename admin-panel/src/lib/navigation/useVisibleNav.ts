"use client";

import { useMemo } from "react";
import { useAuth } from "@/context/AuthContext";
import { can } from "@/lib/auth/permissions";
import { resolveBusinessScope } from "@/lib/auth/scope";
import { NAV_SECTIONS, type NavSection } from "./nav";

/**
 * The sidebar renders exactly what this returns. A nav item the user cannot
 * reach is never rendered — the same permission module backs the route guard,
 * so the nav and the guard can't drift apart.
 */
export function useVisibleNav(): NavSection[] {
	const { user } = useAuth();

	return useMemo(() => {
		const scope = resolveBusinessScope(user);

		return NAV_SECTIONS.map((section) => ({
			...section,
			items: section.items.filter((item) => {
				if (item.permission && !can(user, item.permission)) return false;
				if (item.requiresBusinessScope && scope.kind === "platform") return false;
				return true;
			}),
		})).filter((section) => section.items.length > 0);
	}, [user]);
}
