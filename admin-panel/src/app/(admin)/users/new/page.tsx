import type { Metadata } from "next";
import UserCreateView from "@/features/users/UserCreateView";

export const metadata: Metadata = { title: "New user · Salon Admin" };

export default function NewUserPage() {
	return (
		<UserCreateView
			title="New user"
			description="Create an account and assign its role."
			returnTo="/users"
		/>
	);
}
