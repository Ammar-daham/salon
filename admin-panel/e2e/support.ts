import path from "node:path";
import { expect, type Page } from "@playwright/test";

/** Every account in the demo data (salon-backend's db/seed_data.sql) signs in with this. */
const DEMO_PASSWORD = "Password123!";

export interface Account {
	email: string;
	/** Where the setup project saves this account's session. Git-ignored. */
	storageState: string;
}

export const SUPER_ADMIN: Account = {
	email: "superadmin@salon.example.com",
	storageState: path.join(__dirname, ".auth", "super-admin.json"),
};

/** Anna, the ADMIN of Glow Beauty Studio: open Tuesday to Saturday, with two staff. */
export const SALON_ADMIN: Account = {
	email: "anna.admin@glowbeauty.example.com",
	storageState: path.join(__dirname, ".auth", "salon-admin.json"),
};

export async function signIn(page: Page, email: string, password = DEMO_PASSWORD) {
	await page.goto("/signin");
	// The sign-in form's labels aren't tied to its inputs, so they're found by name.
	await page.locator('input[name="email"]').fill(email);
	await page.locator('input[name="password"]').fill(password);
	await page.getByRole("button", { name: "Sign in" }).click();
}

export async function expectDashboard(page: Page, firstName: string) {
	await expect(page).toHaveURL(/\/$/);
	await expect(
		page.getByRole("heading", { level: 1, name: new RegExp(`^Good (morning|afternoon|evening), ${firstName}$`) }),
	).toBeVisible();
}

/** A toast the panel shows after a change, by its title. */
export function toast(page: Page, title: string) {
	return page.getByRole("status").filter({ hasText: title });
}

/** Tells this run's records apart from earlier runs' against the same database. */
export function uniqueSuffix() {
	return Date.now().toString(36);
}
