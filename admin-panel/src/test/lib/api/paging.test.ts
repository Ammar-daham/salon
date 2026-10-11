import { describe, expect, it } from "vitest";
import { pageParams, toPage, type PageDto } from "@/lib/api/paging";

describe("toPage", () => {
	it("maps each row and carries the totals over in camelCase", () => {
		const dto: PageDto<{ id: number; first_name: string }> = {
			items: [{ id: 7, first_name: "Mia" }],
			page: 2,
			size: 1,
			total_items: 3,
			total_pages: 3,
		};
		expect(toPage(dto, (row) => ({ id: row.id, firstName: row.first_name }))).toEqual({
			items: [{ id: 7, firstName: "Mia" }],
			page: 2,
			size: 1,
			totalItems: 3,
			totalPages: 3,
		});
	});
});

describe("pageParams", () => {
	it("sends the page, size, sort and a trimmed search", () => {
		expect(pageParams({ page: 2, size: 15, q: "  glow ", sort: "-created_at" })).toEqual({
			page: 2,
			size: 15,
			q: "glow",
			sort: "-created_at",
		});
	});

	it("leaves out a blank search and unset filters, so the server doesn't filter on them", () => {
		expect(pageParams({ q: "   " }, { status: undefined, business_id: null, role: "" })).toEqual({});
	});

	it("sends set filters under their API names", () => {
		expect(pageParams({ page: 1 }, { business_id: 2, status: "APPROVED" })).toEqual({
			page: 1,
			business_id: 2,
			status: "APPROVED",
		});
	});
});
