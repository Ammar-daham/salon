import type { Metadata } from "next";
import ResetPasswordForm from "@/components/auth/ResetPasswordForm";

export const metadata: Metadata = {
	title: "Reset password · Salon Admin",
	// The address carries the reset token, so it mustn't travel on in a Referer header.
	referrer: "no-referrer",
};

// The backend's emailed link points here, at app.password-reset.link?token=.
// `searchParams` is a Promise in this version of Next and must be awaited.
export default async function ResetPasswordPage({
	searchParams,
}: {
	searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
	const { token } = await searchParams;
	return <ResetPasswordForm token={(Array.isArray(token) ? token[0] : token) || null} />;
}
