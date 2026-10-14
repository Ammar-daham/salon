import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";

// Unit tests only: pure modules under src/lib/ (mock data, mappers, distance
// math once they exist), so a plain Node environment is enough — no DOM, no
// Next runtime. Tests live under src/test/, mirroring the src/ layout, same
// convention as admin-panel.
export default defineConfig({
	resolve: {
		alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) },
	},
	test: {
		environment: "node",
		include: ["src/test/**/*.test.ts"],
	},
});
