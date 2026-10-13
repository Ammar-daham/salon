import type { Metadata } from "next";
import UserEditView from "@/features/users/UserEditView";

export const metadata: Metadata = { title: "Account · Salonat Pro" };

export default function UserDetailPage() {
	return <UserEditView />;
}
