import type { Id } from "@/lib/api/types";
import type { Role } from "@/lib/resources/auth/auth.types";

export interface User {
	id: Id;
	firstName: string;
	lastName: string;
	email: string | null;
	role: Role;
	createdAt: string;
	updatedAt: string | null;
	/**
	 * NOT returned by the API — User.businessId is WRITE_ONLY server-side. It is
	 * accepted on create and never comes back, so a users table cannot display
	 * which salon a user belongs to. Kept here as write-only for that reason.
	 */
	businessId?: number;
}

export interface CreateUserInput {
	firstName: string;
	lastName: string;
	email: string;
	password: string;
	role: Role;
	/**
	 * Required when the caller is SUPER_ADMIN (they aren't tied to one salon) —
	 * the backend returns 400 without it. Ignored for any other caller, whose
	 * own businessId is substituted server-side.
	 */
	businessId?: number;
}

export interface UpdateUserInput {
	firstName: string;
	lastName: string;
	/** Always send it: UserDataAccessService NPEs into a 400 when role is null. */
	role: Role;
}
