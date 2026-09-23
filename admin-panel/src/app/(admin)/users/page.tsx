import type { Metadata } from "next";
import UserListView from "@/features/users/UserListView";

export const metadata: Metadata = {
	title: "Users · Salon Admin",
	description: "Every account on the platform.",
};

export default function UsersPage() {
	return <UserListView />;
}
