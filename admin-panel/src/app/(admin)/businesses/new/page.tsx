import type { Metadata } from "next";
import BusinessCreateView from "@/features/businesses/BusinessCreateView";

export const metadata: Metadata = {
	title: "New salon · Salon Admin",
};

export default function NewBusinessPage() {
	return <BusinessCreateView />;
}
