"use client";

import { useState, type FormEvent } from "react";
import Alert from "@/components/ui/alert/Alert";
import Button from "@/components/ui/button/Button";
import Field, { TextInput } from "@/components/ui/form/Field";
import { getErrorMessage } from "@/lib/api/errors";
import { useRequestPasswordReset } from "@/lib/resources/auth/auth.hooks";
import AuthPanel from "./AuthPanel";

const BACK_TO_SIGN_IN = { href: "/signin", label: "Back to sign in" };

/**
 * Asks for a reset link. The confirmation reads the same whether or not the email has an
 * account, as the backend's answer does, so this page can't be used to find out who has one.
 */
export default function ForgotPasswordForm() {
	const request = useRequestPasswordReset();
	const [email, setEmail] = useState("");
	const [emailError, setEmailError] = useState<string | null>(null);
	const [failure, setFailure] = useState<string | null>(null);
	const [sentTo, setSentTo] = useState<string | null>(null);

	async function handleSubmit(e: FormEvent<HTMLFormElement>) {
		e.preventDefault();
		const address = email.trim();
		const invalid = !address
			? "Enter the email you sign in with."
			: !/^\S+@\S+\.\S+$/.test(address)
				? "Enter a valid email address."
				: null;
		setEmailError(invalid);
		setFailure(null);
		if (invalid) return;

		try {
			await request.mutateAsync(address);
			setSentTo(address);
		} catch (err) {
			setFailure(getErrorMessage(err));
		}
	}

	if (sentTo) {
		return (
			<AuthPanel
				title="Check your email"
				description={`If ${sentTo} has an account, we've sent it a link to choose a new password.`}
				back={BACK_TO_SIGN_IN}
			>
				<p className="text-sm text-neutral-500 dark:text-neutral-400">
					The link works once, for an hour. Asking again replaces it. If nothing arrives in a few
					minutes, check your spam folder or the address you entered.
				</p>
				<Button
					variant="outline"
					className="mt-6 w-full"
					size="sm"
					onClick={() => setSentTo(null)}
				>
					Use a different email
				</Button>
			</AuthPanel>
		);
	}

	return (
		<AuthPanel
			title="Forgot your password?"
			description="Enter the email you sign in with, and we'll send you a link to choose a new one."
			back={BACK_TO_SIGN_IN}
		>
			{failure && (
				<div className="mb-5">
					<Alert variant="error" title="Couldn't send the link" message={failure} />
				</div>
			)}
			<form onSubmit={handleSubmit} noValidate className="space-y-6">
				<Field label="Email" required error={emailError}>
					{(p) => (
						<TextInput
							{...p}
							type="email"
							value={email}
							onChange={(e) => setEmail(e.target.value)}
							autoComplete="email"
							placeholder="info@gmail.com"
						/>
					)}
				</Field>
				<Button type="submit" className="w-full" size="sm" loading={request.isPending}>
					Send reset link
				</Button>
			</form>
		</AuthPanel>
	);
}
