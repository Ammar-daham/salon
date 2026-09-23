import type { AuthUser } from "@/lib/resources/auth/auth.types";
import type { Id } from "@/lib/api/types";

/**
 * Which salon the current user's data is scoped to.
 *
 * `unresolved` is a legitimate state, not an error: an ADMIN or EMPLOYEE row
 * whose business_id is null in the database. It renders as an explanatory empty
 * state rather than a crash or a blank table.
 *
 * There is deliberately no "pick your salon" dropdown persisted to localStorage.
 * That would be a client-side authorization boundary, which is no boundary at
 * all — and it is unnecessary now that AuthUserResponse carries businessId.
 */
export type BusinessScope =
	| { kind: "platform" }
	| { kind: "business"; businessId: Id }
	| { kind: "unresolved" };

export function resolveBusinessScope(user: AuthUser | null | undefined): BusinessScope {
	if (!user) return { kind: "unresolved" };
	if (user.role === "SUPER_ADMIN") return { kind: "platform" };
	if (user.businessId != null) return { kind: "business", businessId: user.businessId };

	// Local development escape hatch for a user row that predates business linkage.
	const devOverride = process.env.NEXT_PUBLIC_DEV_BUSINESS_ID;
	if (devOverride && !Number.isNaN(Number(devOverride))) {
		return { kind: "business", businessId: Number(devOverride) };
	}

	return { kind: "unresolved" };
}

/** The business id to read/write against, or null when the user is
 *  platform-scoped or unlinked. */
export function scopedBusinessId(user: AuthUser | null | undefined): Id | null {
	const scope = resolveBusinessScope(user);
	return scope.kind === "business" ? scope.businessId : null;
}
