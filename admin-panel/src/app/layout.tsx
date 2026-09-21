import { Outfit } from 'next/font/google';
import './globals.css';
import "flatpickr/dist/flatpickr.css";
import { AuthProvider } from '@/context/AuthContext';
import { SidebarProvider } from '@/context/SidebarContext';
import { ThemeProvider } from '@/context/ThemeContext';
import QueryProvider from '@/lib/query/QueryProvider';
import { ToastProvider } from '@/components/ui/toast/ToastProvider';

const outfit = Outfit({
	subsets: ["latin"],
});

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
		<html lang="en" suppressHydrationWarning>
			<head>
				<script dangerouslySetInnerHTML={{ __html: themeScript }} />
			</head>
			<body className={`${outfit.className} bg-surface text-ink`}>
				<QueryProvider>
					<AuthProvider>
						<ThemeProvider>
							<SidebarProvider>
								<ToastProvider>{children}</ToastProvider>
							</SidebarProvider>
						</ThemeProvider>
					</AuthProvider>
				</QueryProvider>
			</body>
		</html>
	);
}
