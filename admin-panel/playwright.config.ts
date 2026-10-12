import { defineConfig, devices } from "@playwright/test";

/**
 * End-to-end smoke tests (FE-08): a real browser against a running panel, API and database with
 * the demo data loaded, as `docker compose --profile demo up` provides. They start nothing
 * themselves. E2E_BASE_URL points them at a panel somewhere other than http://localhost:3000.
 *
 * The setup project signs each account in once and saves its session; a spec picks the session
 * it needs with `test.use({ storageState })`.
 */
export default defineConfig({
	testDir: "./e2e",
	forbidOnly: !!process.env.CI,
	retries: process.env.CI ? 1 : 0,
	// One at a time: the specs share one database, and a smoke run is short anyway.
	workers: 1,
	reporter: process.env.CI ? [["list"], ["html", { open: "never" }]] : "list",
	use: {
		baseURL: process.env.E2E_BASE_URL ?? "http://localhost:3000",
		trace: "retain-on-failure",
		screenshot: "only-on-failure",
	},
	projects: [
		{ name: "setup", testMatch: /\.setup\.ts$/ },
		{ name: "chromium", use: { ...devices["Desktop Chrome"] }, dependencies: ["setup"] },
	],
});
