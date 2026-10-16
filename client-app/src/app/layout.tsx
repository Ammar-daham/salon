import type { Metadata, Viewport } from "next";
import { Outfit } from "next/font/google";
import "./globals.css";
import { ThemeProvider } from "@/context/ThemeContext";

const outfit = Outfit({
	subsets: ["latin"],
	weight: ["600"],
	variable: "--font-outfit",
});

export const metadata: Metadata = {
	title: "Salonat",
	description: "Find a salon nearby and book your next appointment.",
};

export const viewport: Viewport = {
	width: "device-width",
	initialScale: 1,
	viewportFit: "cover",
	themeColor: [
		{ media: "(prefers-color-scheme: light)", color: "#f7f7f9" },
		{ media: "(prefers-color-scheme: dark)", color: "#000000" },
	],
};

// Runs before first paint so the stored theme is applied without a flash of
// light. ThemeContext reads the same key afterwards and stays in sync.
const themeScript = `
(function() {
  try {
    var stored = localStorage.getItem('theme');
    var dark = stored ? stored === 'dark' : false;
    if (dark) document.documentElement.classList.add('dark');
  } catch (e) {}
})();
`;

export default function RootLayout({
	children,
}: Readonly<{
	children: React.ReactNode;
}>) {
	return (
		<html lang="en" suppressHydrationWarning className={outfit.variable}>
			<head>
				<script dangerouslySetInnerHTML={{ __html: themeScript }} />
			</head>
			<body className="bg-surface text-ink">
				<ThemeProvider>{children}</ThemeProvider>
			</body>
		</html>
	);
}
