import { describe, expect, it } from "vitest";
import { sortParam } from "@/lib/table/useServerTable";

describe("sortParam", () => {
	it("is the column key ascending, and the key with a leading minus descending", () => {
		expect(sortParam({ key: "created_at", direction: "asc" })).toBe("created_at");
		expect(sortParam({ key: "created_at", direction: "desc" })).toBe("-created_at");
	});

	it("is left out when the table isn't sorted, so the server's default applies", () => {
		expect(sortParam(null)).toBeUndefined();
	});
});
