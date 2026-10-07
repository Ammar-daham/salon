import { describe, expect, it } from "vitest";
import { DEFAULT_TIME_ZONE, timeZoneOptions } from "@/lib/utils/timeZones";

describe("timeZoneOptions", () => {
	it("lists the runtime's IANA zones, without fixed offsets", () => {
		const zones = timeZoneOptions(DEFAULT_TIME_ZONE);
		expect(zones).toContain("Europe/Berlin");
		expect(zones).toContain("America/New_York");
		expect(zones.some((zone) => /^[+-]\d/.test(zone))).toBe(false);
	});

	it("keeps the salon's current zone first, even one the runtime doesn't list", () => {
		const zones = timeZoneOptions("Antarctica/Troll-Station");
		expect(zones[0]).toBe("Antarctica/Troll-Station");
		expect(zones.filter((zone) => zone === "Europe/Berlin")).toHaveLength(1);
	});
});
