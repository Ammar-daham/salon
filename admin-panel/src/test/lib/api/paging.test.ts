import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/lib/api/client";
import { fetchAllPages, MAX_PAGE_SIZE, pageParams, toPage, type PageDto } from "@/lib/api/paging";

vi.mock("@/lib/api/client", () => ({ apiClient: { get: vi.fn() } }));

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

describe("fetchAllPages", () => {
	const get = vi.mocked(apiClient.get);
	const page = (items: number[], n: number, totalPages: number) => ({
		data: { items, page: n, size: MAX_PAGE_SIZE, total_items: 3, total_pages: totalPages } satisfies PageDto<number>,
	});

	beforeEach(() => get.mockReset());

	it("asks for full pages, with the filters, until the last one", async () => {
		get.mockResolvedValueOnce(page([1, 2], 1, 2)).mockResolvedValueOnce(page([3], 2, 2));

		await expect(fetchAllPages<number>("/rows", { from: "2026-10-16" })).resolves.toEqual([1, 2, 3]);
		expect(get).toHaveBeenCalledTimes(2);
		expect(get).toHaveBeenLastCalledWith("/rows", {
			params: { from: "2026-10-16", page: 2, size: MAX_PAGE_SIZE },
			signal: undefined,
		});
	});

	it("stops after the first request when the list is empty", async () => {
		get.mockResolvedValueOnce(page([], 1, 0));

		await expect(fetchAllPages<number>("/rows")).resolves.toEqual([]);
		expect(get).toHaveBeenCalledTimes(1);
	});
});
