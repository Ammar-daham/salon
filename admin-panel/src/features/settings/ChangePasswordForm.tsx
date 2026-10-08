"use client";

import { useState, type FormEvent } from "react";
import { useChangePassword } from "@/lib/resources/auth/auth.hooks";
import {
	newPasswordErrors,
	PASSWORD_MAX_LENGTH,
	PASSWORD_MIN_LENGTH,
} from "@/lib/resources/auth/auth.validation";
import { getErrorMessage, isApiError } from "@/lib/api/errors";
import { useToast } from "@/components/ui/toast/ToastProvider";
import Card from "@/components/ui/Card";
import Button from "@/components/ui/button/Button";
import Field, { PasswordInput } from "@/components/ui/form/Field";

type Errors = Partial<Record<"currentPassword" | "password" | "confirmPassword", string>>;

/**
 * The signed-in user's own password. The backend signs out every other session of theirs and
 * keeps this one, so nothing here has to sign in again.
 */
export default function ChangePasswordForm({ email }: { email: string }) {
	const { toast } = useToast();
	const change = useChangePassword();

	const [currentPassword, setCurrentPassword] = useState("");
	const [newPassword, setNewPassword] = useState("");
	const [confirmPassword, setConfirmPassword] = useState("");
	const [errors, setErrors] = useState<Errors>({});

	async function handleSubmit(e: FormEvent<HTMLFormElement>) {
		e.preventDefault();
		const found: Errors = newPasswordErrors(newPassword, confirmPassword);
		if (!currentPassword) found.currentPassword = "Enter your current password.";
		setErrors(found);
		if (Object.keys(found).length > 0) return;

		try {
			await change.mutateAsync({ currentPassword, newPassword });
			setCurrentPassword("");
			setNewPassword("");
			setConfirmPassword("");
			toast({
				tone: "success",
				title: "Password changed",
				description: "You're still signed in here. Anywhere else, sign in again with the new one.",
			});
		} catch (err) {
			// The new password already passed the backend's own rule above, so a 400 means the
			// current one was wrong.
			if (isApiError(err) && err.kind === "validation") {
				setErrors({ currentPassword: err.message });
			} else {
				toast({ tone: "error", title: "Couldn't change your password", description: getErrorMessage(err) });
			}
		}
	}

	return (
		<form onSubmit={handleSubmit}>
			<Card title="Password" description="Changing it signs you out everywhere else you're signed in.">
				{/* Tells a password manager whose password this is. */}
				<input type="email" name="username" autoComplete="username" value={email} readOnly hidden />
				<div className="flex flex-col gap-5">
					<Field label="Current password" required error={errors.currentPassword}>
						{(p) => (
							<PasswordInput
								{...p}
								value={currentPassword}
								onChange={(e) => setCurrentPassword(e.target.value)}
								autoComplete="current-password"
							/>
						)}
					</Field>
					<div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
						<Field
							label="New password"
							required
							error={errors.password}
							hint={`${PASSWORD_MIN_LENGTH} to ${PASSWORD_MAX_LENGTH} characters.`}
						>
							{(p) => (
								<PasswordInput
									{...p}
									value={newPassword}
									onChange={(e) => setNewPassword(e.target.value)}
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
					</div>
					<div className="flex justify-end">
						<Button type="submit" loading={change.isPending}>
							Change password
						</Button>
					</div>
				</div>
			</Card>
		</form>
	);
}
