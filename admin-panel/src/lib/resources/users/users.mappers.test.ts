import { describe, expect, it } from "vitest";
import { toCreateUserRequest, toUpdateUserRequest, toUser, type UserDto } from "./users.mappers";

const dto: UserDto = {
	id: 4,
	first_name: "Mia",
	last_name: "Stylist",
	email: "mia.stylist@glow.test",
	role: "EMPLOYEE",
	created_at: "2026-09-01T10:00:00Z",
	updated_at: "2026-09-02T10:00:00Z",
};

describe("toUser", () => {
	it("maps the snake_case wire shape", () => {
		expect(toUser(dto)).toEqual({
			id: 4,
			firstName: "Mia",
			lastName: "Stylist",
			email: "mia.stylist@glow.test",
			role: "EMPLOYEE",
			createdAt: "2026-09-01T10:00:00Z",
			updatedAt: "2026-09-02T10:00:00Z",
		});
	});

	it("normalises a missing updated_at to null", () => {
		const { updated_at: _omitted, ...withoutUpdatedAt } = dto;
		expect(toUser(withoutUpdatedAt as UserDto).updatedAt).toBeNull();
	});

	it("never produces a businessId — the API doesn't return one", () => {
		expect(toUser({ ...dto, business_id: 1 } as UserDto)).not.toHaveProperty("businessId");
	});
});

describe("toCreateUserRequest", () => {
	const input = {
		firstName: "Mia",
		lastName: "Stylist",
		email: "mia.stylist@glow.test",
		password: "Password123!",
		role: "EMPLOYEE" as const,
	};

	it("sends snake_case and omits business_id when none is chosen", () => {
		expect(toCreateUserRequest(input)).toEqual({
			first_name: "Mia",
			last_name: "Stylist",
			email: "mia.stylist@glow.test",
			password: "Password123!",
			role: "EMPLOYEE",
		});
	});

	it("sends business_id when a super admin picks a salon", () => {
		expect(toCreateUserRequest({ ...input, businessId: 2 })).toMatchObject({ business_id: 2 });
	});
});

describe("toUpdateUserRequest", () => {
	it("sends only the columns the backend updates, always including role", () => {
		expect(toUpdateUserRequest({ firstName: "Mia", lastName: "Stylist", role: "ADMIN" })).toEqual({
			first_name: "Mia",
			last_name: "Stylist",
			role: "ADMIN",
		});
	});
});
