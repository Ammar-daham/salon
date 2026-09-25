import type { AuthUser, Role } from "@/lib/resources/auth/auth.types";

/**
 * Capability names, not role checks. Call sites ask "can this user delete a
 * business?" rather than "is this user a SUPER_ADMIN?", so the day the backend
 * grows per-employee permissions only the body of `can()` changes.
 *
 * We deliberately ship ROLE-LEVEL grants only. The backend has no permissions
 * column, no table and no endpoint, and SecurityConfig gates purely on
 * hasAnyRole — an EMPLOYEE's only write in the entire API is on their own user
 * record. Granular permissions would be fiction, and a permissions-management
 * screen whose settings cannot persist is the worst kind of mock.
 */
export type Permission =
	| "dashboard:view"
	| "business:list"
	| "business:view"
	| "business:create"
	| "business:edit"
	| "business:delete"
	| "user:list"
	| "user:create"
	| "user:edit"
	| "user:delete"
	| "employee:list"
	| "employee:create"
	| "employee:edit"
	| "employee:delete"
	| "customer:list"
	| "customer:create"
	| "customer:edit"
	| "customer:delete"
	| "service:list"
	| "service:create"
	| "service:edit"
	| "service:delete"
	| "appointment:list"
	| "appointment:create"
	| "appointment:edit"
	| "appointment:cancel"
	| "calendar:view"
	| "report:view"
	| "subscription:manage"
	| "subscription:billing"
	| "notification:view"
	| "settings:profile"
	| "settings:business"
	| "settings:platform";

const SUPER_ADMIN_GRANTS: Permission[] = [
	"dashboard:view",
	"business:list", "business:view", "business:create", "business:edit", "business:delete",
	"user:list", "user:create", "user:edit", "user:delete",
	"employee:list", "employee:create", "employee:edit", "employee:delete",
	"customer:list", "customer:create", "customer:edit", "customer:delete",
	"service:list", "service:create", "service:edit", "service:delete",
	"appointment:list", "appointment:create", "appointment:edit", "appointment:cancel",
	"calendar:view",
	"report:view",
	"subscription:manage",
	"notification:view",
	"settings:profile", "settings:business", "settings:platform",
];

const ADMIN_GRANTS: Permission[] = [
	"dashboard:view",
	// No business:list — that page is the platform-wide directory.
	// No business:delete — the backend would let an admin delete their own salon,
	// but removing a salon is a platform decision, and today it also leaves the
	// salon's staff rows dangling (BE-17).
	"business:view", "business:edit",
	// The backend scopes GET /users to the caller's business and checks
	// ownership on PUT/DELETE (BE-01, BE-05), and refuses SUPER_ADMIN
	// promotion (BE-04), so the Users page is safe for an admin.
	"user:list", "user:create", "user:edit", "user:delete",
	"employee:list", "employee:create", "employee:edit", "employee:delete",
	"customer:list", "customer:create", "customer:edit", "customer:delete",
	"service:list", "service:create", "service:edit", "service:delete",
	"appointment:list", "appointment:create", "appointment:edit", "appointment:cancel",
	"calendar:view",
	"report:view",
	"subscription:billing",
	"notification:view",
	"settings:profile", "settings:business",
];

const EMPLOYEE_GRANTS: Permission[] = [
	"dashboard:view",
	"business:view",
	"customer:list", "customer:create",
	"service:list",
	// Scoped to their own appointments at the row level; no delete, cancel only.
	"appointment:list", "appointment:create", "appointment:edit", "appointment:cancel",
	"calendar:view",
	"notification:view",
	"settings:profile",
];

/** Customers never reach the admin panel — AppUserDetailsService rejects users
 *  with a null password_hash, so they cannot even sign in. */
const CUSTOMER_GRANTS: Permission[] = [];

const ROLE_GRANTS: Record<Role, ReadonlySet<Permission>> = {
	SUPER_ADMIN: new Set(SUPER_ADMIN_GRANTS),
	ADMIN: new Set(ADMIN_GRANTS),
	EMPLOYEE: new Set(EMPLOYEE_GRANTS),
	CUSTOMER: new Set(CUSTOMER_GRANTS),
};

export function can(user: AuthUser | null | undefined, perm: Permission): boolean {
	if (!user) return false;
	// When the backend grows per-user permissions this becomes:
	//   return (user.permissions ?? ROLE_GRANTS[user.role]).has(perm);
	// Every call site is already correct.
	return ROLE_GRANTS[user.role].has(perm);
}

export function canAny(
	user: AuthUser | null | undefined,
	perms: readonly Permission[],
): boolean {
	return perms.some((p) => can(user, p));
}

/**
 * Roles this user may grant when creating an account. Mirrors the backend rule
 * in UserService.addUser: an ADMIN attempting to create a SUPER_ADMIN gets a
 * 403, so the option must not be offered.
 */
export function grantableRoles(user: AuthUser | null | undefined): Role[] {
	if (!user) return [];
	if (user.role === "SUPER_ADMIN") return ["EMPLOYEE", "ADMIN", "SUPER_ADMIN"];
	if (user.role === "ADMIN") return ["EMPLOYEE", "ADMIN"];
	return [];
}
