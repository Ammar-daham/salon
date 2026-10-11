import type { Id } from "@/lib/api/types";
import type { CreateEmployeeInput, Employee, UpdateEmployeeInput } from "./employees.types";

export interface StaffDto {
	id: number;
	title: string;
	is_active: boolean;
	user_id: number;
	first_name: string;
	last_name: string;
	/** Only for the salon's admins and super admins; null for anyone else. */
	email: string | null;
	hired_at: string;
	calendar_colour: string | null;
	created_at: string | null;
	updated_at: string | null;
}

interface StaffRequestDto {
	title: string;
	is_active: boolean;
	hired_at: string;
	calendar_colour: string | null;
}

/** Only present in the create body - who this employment record is for can't be changed afterwards. */
interface CreateStaffRequestDto extends StaffRequestDto {
	user_id: Id;
}

/** A staff record names its person; which salon it's at comes from the caller, who asked by salon. */
export function toEmployee(dto: StaffDto, businessId: Id, businessName: string): Employee {
	return {
		id: dto.id,
		firstName: dto.first_name,
		lastName: dto.last_name,
		email: dto.email ?? "",
		title: dto.title,
		isActive: dto.is_active,
		businessId,
		businessName,
		hiredAt: dto.hired_at,
		calendarColour: dto.calendar_colour,
	};
}

export function toCreateStaffRequest(input: CreateEmployeeInput): CreateStaffRequestDto {
	return {
		user_id: input.userId,
		title: input.title,
		is_active: input.isActive,
		hired_at: input.hiredAt,
		calendar_colour: input.calendarColour ?? null,
	};
}

export function toUpdateStaffRequest(input: UpdateEmployeeInput): StaffRequestDto {
	return {
		title: input.title,
		is_active: input.isActive,
		hired_at: input.hiredAt,
		calendar_colour: input.calendarColour ?? null,
	};
}
