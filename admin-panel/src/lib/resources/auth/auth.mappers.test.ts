import { describe, expect, it } from "vitest";
import { toAuthUser } from "./auth.mappers";

describe("toAuthUser", () => {
	it("maps the snake_case /auth/me response, including the business", () => {
		expect(
			toAuthUser({
				id: 2,
				first_name: "Anna",
				last_name: "Admin",
				email: "anna.admin@glow.test",
				role: "ADMIN",
				business_id: 1,
			}),
		).toEqual({
			id: 2,
			firstName: "Anna",
			lastName: "Admin",
			email: "anna.admin@glow.test",
			role: "ADMIN",
			businessId: 1,
		});
	});

	it("keeps a super admin's null business", () => {
		const user = toAuthUser({
			id: 1,
			first_name: "Sam",
			last_name: "Root",
			email: "superadmin@salon.test",
			role: "SUPER_ADMIN",
			business_id: null,
		});
		expect(user.businessId).toBeNull();
	});
});
