import type { Metadata } from "next";
import SettingsView from "@/features/settings/SettingsView";

export const metadata: Metadata = {
	title: "Settings · Salon Admin",
};

export default function SettingsPage() {
	return <SettingsView />;
}
