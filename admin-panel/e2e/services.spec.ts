import { expect, test } from "@playwright/test";
import { SALON_ADMIN, toast, uniqueSuffix } from "./support";

test.use({ storageState: SALON_ADMIN.storageState });

test("a salon admin adds, changes and deletes a service", async ({ page }) => {
	const name = `E2E Service ${uniqueSuffix()}`;
	const row = page.getByRole("row").filter({ hasText: name });

	await test.step("add", async () => {
		await page.goto("/services");
		await page.getByRole("button", { name: "New service" }).first().click();
		const form = page.getByRole("dialog", { name: "New service" });
		await form.getByRole("textbox", { name: "Name", exact: true }).fill(name);
		await form.getByLabel("Duration (minutes)").fill("30");
		await form.getByLabel(/^Price/).fill("20");
		await form.getByRole("button", { name: "Add service" }).click();

		await expect(toast(page, "Service added")).toBeVisible();
		await page.getByLabel("Search services").fill(name);
		await expect(row).toContainText("30 min");
	});

	await test.step("change", async () => {
		await page.getByRole("button", { name: `Edit ${name}` }).click();
		const form = page.getByRole("dialog", { name: "Edit service" });
		await form.getByLabel("Duration (minutes)").fill("40");
		await form.getByRole("button", { name: "Save changes" }).click();

		await expect(toast(page, "Service updated")).toBeVisible();
		await expect(row).toContainText("40 min");
	});

	await test.step("delete", async () => {
		await page.getByRole("button", { name: `Delete ${name}` }).click();
		await page.getByRole("dialog", { name: `Delete ${name}?` }).getByRole("button", { name: "Delete service" }).click();

		await expect(toast(page, "Service deleted")).toBeVisible();
		await expect(row).toHaveCount(0);
	});
});
