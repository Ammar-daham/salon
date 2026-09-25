import type { Metadata } from "next";
import UserListView from "@/features/users/UserListView";

export const metadata: Metadata = {
	title: "Users · Salon Admin",
	description: "User accounts and their roles.",
};

export default function UsersPage() {
	return <UserListView />;
}
