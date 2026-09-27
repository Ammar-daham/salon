import type { AuthUser, Role } from "@/lib/resources/auth/auth.types";

/** A signed-in user with the given role, shaped like the /auth/me response. */
export function userWithRole(role: Role): AuthUser {
	return {
		id: 1,
		firstName: "Test",
		lastName: "User",
		email: "test@salon.test",
		role,
		businessId: role === "SUPER_ADMIN" ? null : 1,
	};
}
