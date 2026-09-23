import type { AuthUser } from "@/lib/resources/auth/auth.types";
import { can, type Permission } from "./permissions";

interface RouteRule {
	pattern: RegExp;
	permission: Permission;
}

/**
 * First matching rule wins, so more specific paths come first.
 *
 * There is no middleware.ts on purpose: the session cookie is an opaque Spring
 * JSESSIONID with no role in it, so middleware could only check that a cookie
 * exists — duplicating the layout's own check while adding an edge runtime.
 * Gating stays client-side and centralised here; the backend enforces for real.
 */
const ROUTE_RULES: RouteRule[] = [
	{ pattern: /^\/settings\/platform(\/|$)/, permission: "settings:platform" },
	{ pattern: /^\/settings\/business(\/|$)/, permission: "settings:business" },
	{ pattern: /^\/settings(\/|$)/, permission: "settings:profile" },
	{ pattern: /^\/subscriptions\/billing(\/|$)/, permission: "subscription:billing" },
	{ pattern: /^\/subscriptions(\/|$)/, permission: "subscription:manage" },
	{ pattern: /^\/notifications(\/|$)/, permission: "notification:view" },
	{ pattern: /^\/reports(\/|$)/, permission: "report:view" },
	{ pattern: /^\/calendar(\/|$)/, permission: "calendar:view" },
	{ pattern: /^\/appointments(\/|$)/, permission: "appointment:list" },
	{ pattern: /^\/services(\/|$)/, permission: "service:list" },
	{ pattern: /^\/customers(\/|$)/, permission: "customer:list" },
	{ pattern: /^\/employees(\/|$)/, permission: "employee:list" },
	{ pattern: /^\/users(\/|$)/, permission: "user:list" },
	{ pattern: /^\/businesses\/new(\/|$)/, permission: "business:create" },
	// The list is platform-wide; a single salon's detail page is not, and is
	// additionally ownership-checked in businesses/[id]/layout.tsx.
	{ pattern: /^\/businesses$/, permission: "business:list" },
	{ pattern: /^\/businesses\/[^/]+/, permission: "business:view" },
	{ pattern: /^\/my-business(\/|$)/, permission: "business:view" },
	{ pattern: /^\/$/, permission: "dashboard:view" },
];

export function canAccessRoute(user: AuthUser | null | undefined, pathname: string): boolean {
	const rule = ROUTE_RULES.find((r) => r.pattern.test(pathname));
	// Unknown paths fall through to the 404 page rather than the forbidden page.
	if (!rule) return true;
	return can(user, rule.permission);
}
