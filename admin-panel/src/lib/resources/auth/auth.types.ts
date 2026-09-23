export type Role = "CUSTOMER" | "EMPLOYEE" | "ADMIN" | "SUPER_ADMIN";

export const ROLE_LABELS: Record<Role, string> = {
	CUSTOMER: "Customer",
	EMPLOYEE: "Employee",
	ADMIN: "Admin",
	SUPER_ADMIN: "Super admin",
};

export interface AuthUser {
	id: number;
	firstName: string;
	lastName: string;
	email: string;
	role: Role;
	/**
	 * Null for SUPER_ADMIN — they are platform-scoped rather than tied to one
	 * salon. Populated for ADMIN and EMPLOYEE.
	 *
	 * This field only exists because AuthUserResponse was extended server-side;
	 * User.businessId is WRITE_ONLY, so no other endpoint can tell the client
	 * which business it belongs to.
	 */
	businessId: number | null;
}
