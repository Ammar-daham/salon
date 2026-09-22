import type { Metadata } from "next";
import UserEditView from "@/features/users/UserEditView";

export const metadata: Metadata = { title: "Account · Salon Admin" };

export default function UserDetailPage() {
	return <UserEditView />;
}
