"use client";

import type React from "react";
import { createContext, useCallback, useContext, useMemo, useSyncExternalStore } from "react";

type Theme = "light" | "dark";

type ThemeContextType = {
	theme: Theme;
	toggleTheme: () => void;
	setTheme: (theme: Theme) => void;
};

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

const STORAGE_KEY = "theme";

/**
 * The `dark` class on <html> is the single source of truth. An inline script in
 * the root layout applies it before first paint, so there is no flash and no
 * need to re-derive the theme in an effect — we just subscribe to the DOM.
 */
function subscribe(onChange: () => void) {
	const observer = new MutationObserver(onChange);
	observer.observe(document.documentElement, {
		attributes: true,
		attributeFilter: ["class"],
	});
	return () => observer.disconnect();
}

function getSnapshot(): Theme {
	return document.documentElement.classList.contains("dark") ? "dark" : "light";
}

function getServerSnapshot(): Theme {
	return "light";
}

function applyTheme(theme: Theme) {
	document.documentElement.classList.toggle("dark", theme === "dark");
	try {
		localStorage.setItem(STORAGE_KEY, theme);
	} catch {
		// Private mode or blocked site data — the class still applies for this
		// session, it just won't be remembered.
	}
}

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
	const theme = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);

	const setTheme = useCallback((next: Theme) => applyTheme(next), []);
	const toggleTheme = useCallback(
		() => applyTheme(document.documentElement.classList.contains("dark") ? "light" : "dark"),
		[],
	);

	const value = useMemo(
		() => ({ theme, toggleTheme, setTheme }),
		[theme, toggleTheme, setTheme],
	);

	return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
};

export const useTheme = () => {
	const context = useContext(ThemeContext);
	if (context === undefined) {
		throw new Error("useTheme must be used within a ThemeProvider");
	}
	return context;
};
