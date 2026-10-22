import { describe, expect, it } from "vitest";
import { statusActions } from "@/lib/resources/businesses/businesses.review";
import type { BusinessStatus } from "@/lib/resources/businesses/businesses.types";

describe("statusActions", () => {
	it.each<[BusinessStatus, string[], BusinessStatus[]]>([
		["PENDING", ["Approve", "Reject"], ["APPROVED", "REJECTED"]],
		["APPROVED", ["Suspend"], ["SUSPENDED"]],
		["SUSPENDED", ["Reinstate"], ["APPROVED"]],
		["REJECTED", ["Approve"], ["APPROVED"]],
	])("offers a %s salon %j", (status, labels, targets) => {
		const actions = statusActions(status);
		expect(actions.map((a) => a.label)).toEqual(labels);
		expect(actions.map((a) => a.to)).toEqual(targets);
	});

	it("never offers the status the salon already has", () => {
		for (const status of ["PENDING", "APPROVED", "SUSPENDED", "REJECTED"] as BusinessStatus[]) {
			expect(statusActions(status).map((a) => a.to)).not.toContain(status);
		}
	});

	it("warns before the moves that shut a salon's staff out or hide the salon", () => {
		expect(statusActions("PENDING").find((a) => a.to === "REJECTED")?.destructive).toBe(true);
		expect(statusActions("APPROVED")[0].destructive).toBe(true);
		expect(statusActions("PENDING")[0].destructive).toBe(false);
	});
});
