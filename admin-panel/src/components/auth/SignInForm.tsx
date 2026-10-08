"use client";
import Input from "@/components/form/input/InputField";
import Label from "@/components/form/Label";
import Alert from "@/components/ui/alert/Alert";
import Button from "@/components/ui/button/Button";
import { useAuth } from "@/context/AuthContext";
import { getErrorMessage } from "@/lib/api/errors";
import { EyeCloseIcon, EyeIcon } from "@/icons";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import AuthPanel from "./AuthPanel";

/** `passwordReset` when arriving from a reset link that just set a new password. */
export default function SignInForm({ passwordReset = false }: { passwordReset?: boolean }) {
	const [showPassword, setShowPassword] = useState(false);
	const [email, setEmail] = useState("");
	const [password, setPassword] = useState("");
	const [error, setError] = useState<string | null>(null);
	const [isSubmitting, setIsSubmitting] = useState(false);
	const { login } = useAuth();
	const router = useRouter();

	async function handleSubmit(e: FormEvent<HTMLFormElement>) {
		e.preventDefault();
		setError(null);
		setIsSubmitting(true);
		try {
			await login(email, password);
			router.push("/");
		} catch (err) {
			setError(getErrorMessage(err));
		} finally {
			setIsSubmitting(false);
		}
	}

	return (
		<AuthPanel
			title="Sign In"
			description="Enter your email and password to sign in!"
			back={{ href: "/", label: "Back to dashboard" }}
		>
			{error ? (
				<div className="mb-5">
					<Alert variant="error" title="Sign in failed" message={error} />
				</div>
			) : (
				passwordReset && (
					<div className="mb-5">
						<Alert
							variant="success"
							title="Password reset"
							message="Sign in with your new password."
						/>
					</div>
				)
			)}
			<form onSubmit={handleSubmit}>
				<div className="space-y-6">
					<div>
						<Label>
							Email <span className="text-error-500">*</span>{" "}
						</Label>
						<Input
							id="email"
							name="email"
							placeholder="info@gmail.com"
							type="email"
							defaultValue={email}
							onChange={(e) => setEmail(e.target.value)}
						/>
					</div>
					<div>
						<div className="mb-1.5 flex items-center justify-between gap-4">
							<Label className="mb-0">
								Password <span className="text-error-500">*</span>{" "}
							</Label>
							<Link
								href="/forgot-password"
								className="text-sm font-medium text-primary-700 hover:underline dark:text-primary-300"
							>
								Forgot password?
							</Link>
						</div>
						<div className="relative">
							<Input
								id="password"
								name="password"
								type={showPassword ? "text" : "password"}
								placeholder="Enter your password"
								defaultValue={password}
								onChange={(e) => setPassword(e.target.value)}
							/>
							<span
								onClick={() => setShowPassword(!showPassword)}
								className="absolute z-30 -translate-y-1/2 cursor-pointer right-4 top-1/2"
							>
								{showPassword ? (
									<EyeIcon className="fill-neutral-500 dark:fill-neutral-400" />
								) : (
									<EyeCloseIcon className="fill-neutral-500 dark:fill-neutral-400" />
								)}
							</span>
						</div>
					</div>
					<div>
						<Button type="submit" className="w-full" size="sm" loading={isSubmitting}>
							{isSubmitting ? "Signing in..." : "Sign in"}
						</Button>
					</div>
				</div>
			</form>
		</AuthPanel>
	);
}
