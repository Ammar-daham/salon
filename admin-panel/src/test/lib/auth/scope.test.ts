import { afterEach, describe, expect, it, vi } from "vitest";
import { userWithRole } from "@/test/fixtures";
import { resolveBusinessScope, scopedBusinessId } from "@/lib/auth/scope";

describe("resolveBusinessScope", () => {
	afterEach(() => {
		vi.unstubAllEnvs();
	});

	it("puts a SUPER_ADMIN on the platform", () => {
		expect(resolveBusinessScope(userWithRole("SUPER_ADMIN"))).toEqual({ kind: "platform" });
	});

	it("scopes ADMIN and EMPLOYEE to their own salon", () => {
		for (const role of ["ADMIN", "EMPLOYEE"] as const) {
			const user = { ...userWithRole(role), businessId: 7 };
			expect(resolveBusinessScope(user)).toEqual({ kind: "business", businessId: 7 });
			expect(scopedBusinessId(user)).toBe(7);
		}
	});

	// FE-16: NEXT_PUBLIC_DEV_BUSINESS_ID used to scope a user without a salon to the one it named.
	it("leaves a user without a salon unresolved, whatever the environment says", () => {
		vi.stubEnv("NEXT_PUBLIC_DEV_BUSINESS_ID", "3");
		const user = { ...userWithRole("ADMIN"), businessId: null };
		expect(resolveBusinessScope(user)).toEqual({ kind: "unresolved" });
		expect(scopedBusinessId(user)).toBeNull();
	});

	it("leaves a signed-out visitor unresolved", () => {
		expect(resolveBusinessScope(null)).toEqual({ kind: "unresolved" });
	});
});
