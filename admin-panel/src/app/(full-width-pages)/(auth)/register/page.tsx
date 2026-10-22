import type { Metadata } from "next";
import RegisterBusinessForm from "@/components/auth/RegisterBusinessForm";

export const metadata: Metadata = { title: "Register your salon · Salonat Pro" };

export default function RegisterPage() {
	return <RegisterBusinessForm />;
}
