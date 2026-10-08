import { describe, expect, it } from "vitest";
import { newPasswordErrors } from "@/lib/resources/auth/auth.validation";

describe("newPasswordErrors", () => {
	it("accepts 8 to 72 characters, confirmed", () => {
		expect(newPasswordErrors("a".repeat(8), "a".repeat(8))).toEqual({});
		expect(newPasswordErrors("a".repeat(72), "a".repeat(72))).toEqual({});
	});

	it("refuses fewer than 8 or more than 72 characters, as the backend does", () => {
		expect(newPasswordErrors("a".repeat(7), "a".repeat(7))).toEqual({ password: "Use at least 8 characters." });
		expect(newPasswordErrors("a".repeat(73), "a".repeat(73))).toEqual({ password: "Use at most 72 characters." });
	});

	it("refuses an empty password, or one of only spaces, which the backend treats as blank", () => {
		expect(newPasswordErrors("", "")).toEqual({ password: "Password is required." });
		expect(newPasswordErrors(" ".repeat(10), " ".repeat(10))).toEqual({ password: "Password is required." });
	});

	it("points at the confirmation when it doesn't match", () => {
		expect(newPasswordErrors("correct horse", "correct hose")).toEqual({
			confirmPassword: "Passwords don't match.",
		});
	});
});
