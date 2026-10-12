import { test as setup } from "@playwright/test";
import { SALON_ADMIN, SUPER_ADMIN, expectDashboard, signIn } from "./support";

setup("sign in as the super admin", async ({ page }) => {
	await signIn(page, SUPER_ADMIN.email);
	await expectDashboard(page, "Sam");
	await page.context().storageState({ path: SUPER_ADMIN.storageState });
});

setup("sign in as the salon admin", async ({ page }) => {
	await signIn(page, SALON_ADMIN.email);
	await expectDashboard(page, "Anna");
	await page.context().storageState({ path: SALON_ADMIN.storageState });
});
