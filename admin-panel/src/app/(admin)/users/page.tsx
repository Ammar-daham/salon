import type { Metadata } from "next";
import UserListView from "@/features/users/UserListView";

export const metadata: Metadata = {
	title: "Users · Salonat Pro",
	description: "User accounts and their roles.",
};

export default function UsersPage() {
	return <UserListView />;
}
