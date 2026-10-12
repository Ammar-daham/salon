import { expect, test } from "@playwright/test";
import { SUPER_ADMIN, expectDashboard, signIn, uniqueSuffix } from "./support";

test("signs in, and out again", async ({ page }) => {
	await signIn(page, SUPER_ADMIN.email);
	await expectDashboard(page, "Sam");

	await page.getByRole("button", { name: /Sam/ }).click();
	await page.getByRole("button", { name: "Sign out" }).click();
	await expect(page).toHaveURL(/\/signin$/);

	// Signed out, the panel sends the dashboard back to sign-in.
	await page.goto("/");
	await expect(page).toHaveURL(/\/signin$/);
});

test("turns down a wrong password", async ({ page }) => {
	// An address with no account: five failures lock an email out for a while (BE-22), and a
	// demo account locked out would fail the next run's sign-ins.
	await signIn(page, `nobody-${uniqueSuffix()}@salon.example.com`, "not-the-password");

	await expect(page.getByText("Sign in failed")).toBeVisible();
	await expect(page).toHaveURL(/\/signin$/);
});
