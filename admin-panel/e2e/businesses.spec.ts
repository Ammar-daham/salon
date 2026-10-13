import { expect, test } from "@playwright/test";
import { SUPER_ADMIN, toast, uniqueSuffix } from "./support";

test.use({ storageState: SUPER_ADMIN.storageState });

// A one-pixel GIF: a salon needs an image URL, and this one loads nothing from the network.
const IMAGE = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";

test("a super admin creates, edits and deletes a salon", async ({ page }) => {
	const name = `E2E Salon ${uniqueSuffix()}`;
	const description = "Opened by the end-to-end smoke test.";

	await test.step("create", async () => {
		await page.goto("/businesses");
		await page.getByRole("button", { name: "New salon" }).click();
		await page.getByRole("textbox", { name: "Name", exact: true }).fill(name);
		await page.getByLabel("Image URL").fill(IMAGE);
		await page.getByRole("button", { name: "Create salon" }).click();

		await expect(toast(page, "Salon created")).toBeVisible();
		await expect(page).toHaveURL(/\/businesses\/\d+$/);
		await expect(page.getByRole("heading", { level: 1, name })).toBeVisible();
	});

	await test.step("edit", async () => {
		await page.getByRole("navigation", { name: "Sections" }).getByRole("link", { name: "Settings" }).click();
		await page.getByLabel("Description").fill(description);
		await page.getByRole("button", { name: "Save changes" }).click();
		await expect(toast(page, "Changes saved")).toBeVisible();

		await page.reload();
		await expect(page.getByLabel("Description")).toHaveValue(description);
	});

	await test.step("delete", async () => {
		await page.goto("/businesses");
		await page.getByLabel("Search salons").fill(name);
		await page.getByRole("button", { name: `Delete ${name}` }).click();
		await page.getByRole("dialog", { name: `Delete ${name}?` }).getByRole("button", { name: "Delete salon" }).click();

		await expect(toast(page, "Salon deleted")).toBeVisible();
		await expect(page.getByRole("link", { name })).toHaveCount(0);
	});
});
