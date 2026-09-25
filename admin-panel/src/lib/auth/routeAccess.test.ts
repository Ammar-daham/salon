import { describe, expect, it } from "vitest";
import type { Role } from "@/lib/resources/auth/auth.types";
import { userWithRole } from "@/test/fixtures";
import { canAccessRoute } from "./routeAccess";

type Access = Record<Exclude<Role, "CUSTOMER">, boolean>;

const all: Access = { SUPER_ADMIN: true, ADMIN: true, EMPLOYEE: true };
const superOnly: Access = { SUPER_ADMIN: true, ADMIN: false, EMPLOYEE: false };
const admins: Access = { SUPER_ADMIN: true, ADMIN: true, EMPLOYEE: false };

/** Path → which signed-in roles may open it. */
const ROUTES: [string, Access][] = [
	["/", all],
	["/appointments", all],
	["/calendar", all],
	["/customers", all],
	["/customers/7/notes", all],
	["/services", all],
	["/employees", admins],
	["/employees/new", admins],
	["/users", admins],
	["/users/new", admins],
	["/users/42", admins],
	["/reports", admins],
	["/notifications", all],
	["/my-business", all],
	// /businesses is the platform directory; a single salon is not.
	["/businesses", superOnly],
	["/businesses/new", superOnly],
	["/businesses/3", all],
	["/businesses/3/services", all],
	// More specific rules must win over their prefixes.
	["/settings", all],
	["/settings/business", admins],
	["/settings/platform", superOnly],
	["/subscriptions", superOnly],
	["/subscriptions/billing", { SUPER_ADMIN: false, ADMIN: true, EMPLOYEE: false }],
];

describe("canAccessRoute", () => {
	const cases = ROUTES.flatMap(([path, access]) =>
		(Object.entries(access) as [Role, boolean][]).map(([role, expected]) => ({ path, role, expected })),
	);

	it.each(cases)("$role → $path is $expected", ({ path, role, expected }) => {
		expect(canAccessRoute(userWithRole(role), path)).toBe(expected);
	});

	it("blocks every known route for customers and signed-out visitors", () => {
		for (const [path] of ROUTES) {
			expect(canAccessRoute(userWithRole("CUSTOMER"), path)).toBe(false);
			expect(canAccessRoute(null, path)).toBe(false);
		}
	});

	it("lets unknown paths through so they reach the 404 page", () => {
		expect(canAccessRoute(null, "/does-not-exist")).toBe(true);
		expect(canAccessRoute(userWithRole("EMPLOYEE"), "/usersettings")).toBe(true);
	});
});
