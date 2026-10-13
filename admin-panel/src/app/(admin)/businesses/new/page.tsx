import type { Metadata } from "next";
import BusinessCreateView from "@/features/businesses/BusinessCreateView";

export const metadata: Metadata = {
	title: "New salon · Salonat Pro",
};

export default function NewBusinessPage() {
	return <BusinessCreateView />;
}
