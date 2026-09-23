"use client";
import Input from "@/components/form/input/InputField";
import Label from "@/components/form/Label";
import Alert from "@/components/ui/alert/Alert";
import Button from "@/components/ui/button/Button";
import { useAuth } from "@/context/AuthContext";
import { getErrorMessage } from "@/lib/api/errors";
import { ChevronLeftIcon, EyeCloseIcon, EyeIcon } from "@/icons";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";

export default function SignInForm() {
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
		<div className="flex flex-col flex-1 lg:w-1/2 w-full">
			<div className="w-full max-w-md sm:pt-10 mx-auto mb-5">
				<Link
					href="/"
					className="inline-flex items-center text-sm text-neutral-500 transition-colors hover:text-neutral-700 dark:text-neutral-400 dark:hover:text-neutral-300"
				>
					<ChevronLeftIcon />
					Back to dashboard
				</Link>
			</div>
			<div className="flex flex-col justify-center flex-1 w-full max-w-md mx-auto">
				<div>
					<div className="mb-5 sm:mb-8">
						<h1 className="mb-2 font-semibold text-neutral-800 text-h1 dark:text-white/90 sm:text-display">
							Sign In
						</h1>
						<p className="text-sm text-neutral-500 dark:text-neutral-400">
							Enter your email and password to sign in!
						</p>
					</div>
					{error && (
						<div className="mb-5">
							<Alert variant="error" title="Sign in failed" message={error} />
						</div>
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
								<Label>
									Password <span className="text-error-500">*</span>{" "}
								</Label>
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
				</div>
			</div>
		</div>
	);
}
