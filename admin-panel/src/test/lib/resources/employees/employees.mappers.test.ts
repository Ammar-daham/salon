import { describe, expect, it } from "vitest";
import {
	toCreateStaffRequest,
	toEmployee,
	toUpdateStaffRequest,
	type StaffDto,
} from "@/lib/resources/employees/employees.mappers";

const dto: StaffDto = {
	id: 1,
	title: "Senior Stylist",
	is_active: true,
	user_id: 4,
	hired_at: "2021-03-15",
	calendar_colour: "#FF5733",
	created_at: "2026-09-01T10:00:00Z",
	updated_at: null,
};

const user = { firstName: "Mia", lastName: "Stylist", email: "mia.stylist@glow.test" };

describe("toEmployee", () => {
	it("maps the staff DTO plus the caller-supplied business and joined user", () => {
		expect(toEmployee(dto, 1, "Glow Beauty Studio", user)).toEqual({
			id: 1,
			firstName: "Mia",
			lastName: "Stylist",
			email: "mia.stylist@glow.test",
			title: "Senior Stylist",
			isActive: true,
			businessId: 1,
			businessName: "Glow Beauty Studio",
			hiredAt: "2021-03-15",
			calendarColour: "#FF5733",
		});
	});

	it("normalises a null email to an empty string", () => {
		expect(toEmployee(dto, 1, "Glow Beauty Studio", { ...user, email: null }).email).toBe("");
	});
});

describe("toCreateStaffRequest", () => {
	it("sends snake_case, including the user being hired", () => {
		expect(
			toCreateStaffRequest({
				userId: 4,
				title: "Senior Stylist",
				isActive: true,
				hiredAt: "2021-03-15",
				calendarColour: "#FF5733",
			}),
		).toEqual({
			user_id: 4,
			title: "Senior Stylist",
			is_active: true,
			hired_at: "2021-03-15",
			calendar_colour: "#FF5733",
		});
	});

	it("sends a null calendar_colour rather than omitting it", () => {
		expect(
			toCreateStaffRequest({
				userId: 4,
				title: "Senior Stylist",
				isActive: true,
				hiredAt: "2021-03-15",
			}),
		).toMatchObject({ calendar_colour: null });
	});
});

describe("toUpdateStaffRequest", () => {
	it("sends every editable column but never user_id - who this record is for can't change", () => {
		expect(
			toUpdateStaffRequest({
				title: "Lead Stylist",
				isActive: false,
				hiredAt: "2021-03-15",
				calendarColour: null,
			}),
		).toEqual({
			title: "Lead Stylist",
			is_active: false,
			hired_at: "2021-03-15",
			calendar_colour: null,
		});
	});
});
