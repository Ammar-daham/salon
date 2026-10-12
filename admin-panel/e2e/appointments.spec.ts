import { expect, test, type Locator, type Page } from "@playwright/test";
import { SALON_ADMIN, toast } from "./support";

test.use({ storageState: SALON_ADMIN.storageState });

/** A Wednesday at least a week away, when Glow Beauty Studio and both its staff work all day. */
function openDay(): string {
	const day = new Date();
	day.setUTCDate(day.getUTCDate() + 7);
	while (day.getUTCDay() !== 3) day.setUTCDate(day.getUTCDate() + 1);
	return day.toISOString().slice(0, 10);
}

/** Picks an option by part of its text: the labels also carry phone numbers and prices. */
async function choose(select: Locator, text: string) {
	const option = select.locator("option", { hasText: text });
	await expect(option).toHaveCount(1);
	await select.selectOption((await option.getAttribute("value"))!);
}

/** Clicks the first open time not at `except`, and returns its time, "HH:MM". */
async function pickOpenTime(page: Page, except?: string): Promise<string> {
	const slots = page.getByRole("button", { name: / at \d{2}:\d{2}$/ });
	await expect(slots.first()).toBeVisible();
	for (const slot of await slots.all()) {
		const time = (await slot.getAttribute("aria-label"))!.slice(-5);
		if (time !== except) {
			await slot.click();
			return time;
		}
	}
	throw new Error(`No open time other than ${except}`);
}

test("a salon admin books, changes and cancels an appointment", async ({ page }) => {
	const heading = page.getByRole("heading", { level: 1, name: "Signature Haircut" });
	let bookedAt = "";

	await test.step("book", async () => {
		await page.goto(`/appointments/new?date=${openDay()}`);
		await choose(page.getByRole("combobox", { name: "Client", exact: true }), "Olivia Customer");
		await choose(page.getByRole("combobox", { name: "Service", exact: true }), "Signature Haircut");
		bookedAt = await pickOpenTime(page);
		await page.getByLabel("Notes").fill("Booked by the end-to-end smoke test.");
		await page.getByRole("button", { name: "Book appointment" }).click();

		await expect(toast(page, "Appointment booked")).toBeVisible();
		await expect(page).toHaveURL(/\/appointments\/\d+/);
		await expect(heading).toBeVisible();
		await expect(page.getByText(`, ${bookedAt}–`)).toBeVisible();
	});

	await test.step("change the time and notes", async () => {
		await page.getByRole("button", { name: "Edit", exact: true }).click();
		const movedTo = await pickOpenTime(page, bookedAt);
		await page.getByLabel("Notes").fill("Moved by the end-to-end smoke test.");
		await page.getByRole("button", { name: "Save changes" }).click();

		await expect(toast(page, "Appointment updated")).toBeVisible();
		await expect(heading).toBeVisible();
		await expect(page.getByText(`, ${movedTo}–`)).toBeVisible();
		await expect(page.getByText("Moved by the end-to-end smoke test.")).toBeVisible();
	});

	await test.step("cancel", async () => {
		// Appointments are never deleted: a cancelled one stays in the client's history.
		await page.getByRole("button", { name: "Cancel appointment" }).click();
		await page
			.getByRole("dialog", { name: "Cancel this appointment?" })
			.getByRole("button", { name: "Cancel appointment" })
			.click();

		await expect(toast(page, "Appointment cancelled")).toBeVisible();
		await expect(page.getByText("Cancelled", { exact: true })).toBeVisible();
		await expect(page.getByRole("button", { name: "Edit", exact: true })).toHaveCount(0);
	});
});
