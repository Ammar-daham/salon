import type { Id } from "@/lib/api/types";
import type { Weekday } from "@/lib/mock/pools";

export interface WorkingDay {
	day: Weekday;
	/** null when the employee doesn't work that day. */
	start: string | null;
	end: string | null;
}

export interface Employee {
	id: Id;
	firstName: string;
	lastName: string;
	email: string;
	/** Job title. The `staff` table has this column, but its data access layer is
	 *  stubbed out and isn't even a Spring bean, so nothing can read or write it. */
	title: string;
	isActive: boolean;
	businessId: Id;
	businessName: string;
	hiredAt: string;
	workingHours: WorkingDay[];
	/** Bookings this week — meaningless until appointments exist. */
	appointmentsThisWeek: number;
}

export function employeeFullName(e: Pick<Employee, "firstName" | "lastName">) {
	return `${e.firstName} ${e.lastName}`;
}
