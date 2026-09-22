import type { Metadata } from "next";
import MyBusinessRedirect from "@/features/businesses/MyBusinessRedirect";

export const metadata: Metadata = {
	title: "My salon · Salon Admin",
};

export default function MyBusinessPage() {
	return <MyBusinessRedirect />;
}
