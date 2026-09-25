import { describe, expect, it } from "vitest";
import type { Role } from "@/lib/resources/auth/auth.types";
import { userWithRole } from "@/test/fixtures";
import { can, canAny, grantableRoles, type Permission } from "./permissions";

const S: Role = "SUPER_ADMIN";
const A: Role = "ADMIN";
const E: Role = "EMPLOYEE";

/**
 * The whole matrix, written out by hand: which roles hold each permission.
 * Keyed by Permission, so adding a permission without deciding who gets it
 * fails `npm run typecheck`. CUSTOMER holds nothing — customers can't sign in.
 */
const MATRIX: Record<Permission, Role[]> = {
	"dashboard:view": [S, A, E],
	"business:list": [S],
	"business:view": [S, A, E],
	"business:create": [S],
	"business:edit": [S, A],
	"business:delete": [S],
	"user:list": [S, A],
	"user:create": [S, A],
	"user:edit": [S, A],
	"user:delete": [S, A],
	"employee:list": [S, A],
	"employee:create": [S, A],
	"employee:edit": [S, A],
	"employee:delete": [S, A],
	"customer:list": [S, A, E],
	"customer:create": [S, A, E],
	"customer:edit": [S, A],
	"customer:delete": [S, A],
	"service:list": [S, A, E],
	"service:create": [S, A],
	"service:edit": [S, A],
	"service:delete": [S, A],
	"appointment:list": [S, A, E],
	"appointment:create": [S, A, E],
	"appointment:edit": [S, A, E],
	"appointment:cancel": [S, A, E],
	"calendar:view": [S, A, E],
	"report:view": [S, A],
	"subscription:manage": [S],
	"subscription:billing": [A],
	"notification:view": [S, A, E],
	"settings:profile": [S, A, E],
	"settings:business": [S, A],
	"settings:platform": [S],
};

const ROLES: Role[] = ["SUPER_ADMIN", "ADMIN", "EMPLOYEE", "CUSTOMER"];

describe("can", () => {
	const cases = (Object.entries(MATRIX) as [Permission, Role[]][]).flatMap(([perm, holders]) =>
		ROLES.map((role) => ({ role, perm, expected: holders.includes(role) })),
	);

	it.each(cases)("$role / $perm → $expected", ({ role, perm, expected }) => {
		expect(can(userWithRole(role), perm)).toBe(expected);
	});

	it("denies everything when signed out", () => {
		for (const perm of Object.keys(MATRIX) as Permission[]) {
			expect(can(null, perm)).toBe(false);
			expect(can(undefined, perm)).toBe(false);
		}
	});

	it("keeps platform-level actions away from a salon admin", () => {
		const admin = userWithRole("ADMIN");
		for (const perm of [
			"business:list",
			"business:create",
			"business:delete",
			"subscription:manage",
			"settings:platform",
		] as Permission[]) {
			expect(can(admin, perm)).toBe(false);
		}
	});
});

describe("canAny", () => {
	it("is true when any one permission is held", () => {
		expect(canAny(userWithRole("EMPLOYEE"), ["user:list", "calendar:view"])).toBe(true);
	});

	it("is false when none are held, or the list is empty", () => {
		expect(canAny(userWithRole("EMPLOYEE"), ["user:list", "report:view"])).toBe(false);
		expect(canAny(userWithRole("SUPER_ADMIN"), [])).toBe(false);
		expect(canAny(null, ["dashboard:view"])).toBe(false);
	});
});

describe("grantableRoles", () => {
	it("lets a super admin grant every staff role", () => {
		expect(grantableRoles(userWithRole("SUPER_ADMIN"))).toEqual(["EMPLOYEE", "ADMIN", "SUPER_ADMIN"]);
	});

	it("never offers SUPER_ADMIN to an admin (the backend 403s it)", () => {
		expect(grantableRoles(userWithRole("ADMIN"))).toEqual(["EMPLOYEE", "ADMIN"]);
	});

	it("offers nothing to anyone else", () => {
		expect(grantableRoles(userWithRole("EMPLOYEE"))).toEqual([]);
		expect(grantableRoles(userWithRole("CUSTOMER"))).toEqual([]);
		expect(grantableRoles(null)).toEqual([]);
	});
});
