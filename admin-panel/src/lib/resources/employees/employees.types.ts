import type { Id } from "@/lib/api/types";

export interface Employee {
	id: Id;
	firstName: string;
	lastName: string;
	email: string;
	title: string;
	isActive: boolean;
	businessId: Id;
	businessName: string;
	/** ISO date ("YYYY-MM-DD"), not a timestamp - staff.hired_at is a DATE column. */
	hiredAt: string;
	/** Hex colour for a future calendar view (DB-04). Editable from Employee settings,
	 *  but nothing renders it yet - no calendar view exists to read it. */
	calendarColour: string | null;
}

export function employeeFullName(e: Pick<Employee, "firstName" | "lastName">) {
	return `${e.firstName} ${e.lastName}`;
}

export interface CreateEmployeeInput {
	/** The user account this employment record is for - it must already exist,
	 *  belong to this business, and hold an employable role (EMPLOYEE/ADMIN). */
	userId: Id;
	title: string;
	isActive: boolean;
	hiredAt: string;
	calendarColour?: string | null;
}

/** Who the record belongs to isn't editable - delete and recreate instead. */
export interface UpdateEmployeeInput {
	title: string;
	isActive: boolean;
	hiredAt: string;
	calendarColour?: string | null;
}
