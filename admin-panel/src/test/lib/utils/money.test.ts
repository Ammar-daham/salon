import { describe, expect, it } from "vitest";
import { formatMoney } from "@/lib/utils/money";

describe("formatMoney", () => {
	it("formats in the salon's own currency rather than a hard-coded one", () => {
		const eur = formatMoney(45, "EUR");
		const sek = formatMoney(45, "SEK");
		expect(eur).not.toEqual(sek);
		expect(eur).toMatch(/€|EUR/);
		expect(sek).toMatch(/kr|SEK/);
	});
});
