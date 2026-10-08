"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import Alert from "@/components/ui/alert/Alert";
import Button from "@/components/ui/button/Button";
import Field, { PasswordInput } from "@/components/ui/form/Field";
import { getErrorMessage, isApiError } from "@/lib/api/errors";
import { useResetPassword } from "@/lib/resources/auth/auth.hooks";
import {
	newPasswordErrors,
	PASSWORD_MAX_LENGTH,
	PASSWORD_MIN_LENGTH,
} from "@/lib/resources/auth/auth.validation";
import AuthPanel from "./AuthPanel";

const BACK_TO_SIGN_IN = { href: "/signin", label: "Back to sign in" };

type Errors = Partial<Record<"password" | "confirmPassword", string>>;

/**
 * Where the emailed link lands (the backend's app.password-reset.link), with its `?token=`.
 * The token is only checked when the new password is sent, since checking it is spending it.
 */
export default function ResetPasswordForm({ token }: { token: string | null }) {
	if (token) return <NewPasswordForm token={token} />;
	return (
		<AuthPanel
			title="Reset your password"
			description="This page needs the link from your reset email."
			back={BACK_TO_SIGN_IN}
		>
			<Alert
				variant="error"
				title="This link is incomplete"
				message="Open the link from the email again, or ask for a new one."
				showLink
				linkHref="/forgot-password"
				linkText="Ask for a new link"
			/>
		</AuthPanel>
	);
}

function NewPasswordForm({ token }: { token: string }) {
	const router = useRouter();
	const reset = useResetPassword();
	const [password, setPassword] = useState("");
	const [confirmPassword, setConfirmPassword] = useState("");
	const [errors, setErrors] = useState<Errors>({});
	const [failure, setFailure] = useState<{ message: string; linkDead: boolean } | null>(null);

	async function handleSubmit(e: FormEvent<HTMLFormElement>) {
		e.preventDefault();
		const found = newPasswordErrors(password, confirmPassword);
		setErrors(found);
		setFailure(null);
		if (Object.keys(found).length > 0) return;

		try {
			await reset.mutateAsync({ token, password });
			// Replaced, so Back doesn't return to a link that no longer works.
			router.replace("/signin?reset=1");
		} catch (err) {
			// The password already passed the backend's own rule, so a 400 is the token.
			setFailure({
				message: getErrorMessage(err),
				linkDead: isApiError(err) && err.kind === "validation",
			});
		}
	}

	return (
		<AuthPanel
			title="Choose a new password"
			description="You'll be signed out wherever you're signed in, then sign in with the new one."
			back={BACK_TO_SIGN_IN}
		>
			{failure && (
				<div className="mb-5">
					<Alert
						variant="error"
						title="Couldn't reset your password"
						message={failure.message}
						showLink={failure.linkDead}
						linkHref="/forgot-password"
						linkText="Ask for a new link"
					/>
				</div>
			)}
			<form onSubmit={handleSubmit} className="space-y-6">
				<Field
					label="New password"
					required
					error={errors.password}
					hint={`${PASSWORD_MIN_LENGTH} to ${PASSWORD_MAX_LENGTH} characters.`}
				>
					{(p) => (
						<PasswordInput
							{...p}
							value={password}
							onChange={(e) => setPassword(e.target.value)}
							autoComplete="new-password"
						/>
					)}
				</Field>
				<Field label="Confirm new password" required error={errors.confirmPassword}>
					{(p) => (
						<PasswordInput
							{...p}
							value={confirmPassword}
							onChange={(e) => setConfirmPassword(e.target.value)}
							autoComplete="new-password"
						/>
					)}
				</Field>
				<Button type="submit" className="w-full" size="sm" loading={reset.isPending}>
					Reset password
				</Button>
			</form>
		</AuthPanel>
	);
}
