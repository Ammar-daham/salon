import { describe, expect, it } from "vitest";
import { cn } from "@/lib/utils/cn";

describe("cn", () => {
	it("joins truthy class values with a space", () => {
		expect(cn("a", "b", "c")).toBe("a b c");
	});

	it("drops falsy values", () => {
		expect(cn("a", false, null, undefined, "b")).toBe("a b");
	});

	it("flattens nested arrays", () => {
		expect(cn("a", ["b", ["c", false, "d"]])).toBe("a b c d");
	});

	it("lets a later conflicting Tailwind utility win", () => {
		expect(cn("px-2 py-1", "px-4")).toBe("py-1 px-4");
	});
});
