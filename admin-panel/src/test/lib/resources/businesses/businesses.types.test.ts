import { describe, expect, it } from "vitest";
import { businessListHref, businessStatusParam } from "@/lib/resources/businesses/businesses.types";

describe("businessStatusParam", () => {
	it("reads a status from ?status=", () => {
		expect(businessStatusParam("PENDING")).toBe("PENDING");
		expect(businessStatusParam("SUSPENDED")).toBe("SUSPENDED");
	});

	it("ignores anything that isn't one status", () => {
		expect(businessStatusParam(undefined)).toBeUndefined();
		expect(businessStatusParam("")).toBeUndefined();
		expect(businessStatusParam("pending")).toBeUndefined();
		expect(businessStatusParam("DELETED")).toBeUndefined();
		expect(businessStatusParam(["PENDING", "APPROVED"])).toBeUndefined();
	});
});

describe("businessListHref", () => {
	it("filters the list to one status, or shows them all", () => {
		expect(businessListHref("PENDING")).toBe("/businesses?status=PENDING");
		expect(businessListHref()).toBe("/businesses");
	});

	it("round-trips through businessStatusParam", () => {
		const query = new URL(businessListHref("REJECTED"), "http://panel").searchParams.get("status") ?? undefined;
		expect(businessStatusParam(query)).toBe("REJECTED");
	});
});
