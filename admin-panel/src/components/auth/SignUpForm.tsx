"use client";
import Input from "@/components/form/input/InputField";
import Label from "@/components/form/Label";
import Alert from "@/components/ui/alert/Alert";
import Button from "@/components/ui/button/Button";
import { useAuth } from "@/context/AuthContext";
import { getAuthErrorMessage } from "@/app/api/auth";
import { createUser } from "@/app/api/users";
import { Role } from "@/app/api/auth";
import { ChevronLeftIcon, EyeCloseIcon, EyeIcon } from "@/icons";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";

const ROLE_LABELS: Record<Role, string> = {
	CUSTOMER: "Customer",
	EMPLOYEE: "Employee",
	ADMIN: "Admin",
	SUPER_ADMIN: "Super Admin",
};

function grantableRoles(callerRole: Role | undefined): Role[] {
	return callerRole === "SUPER_ADMIN" ? ["EMPLOYEE", "ADMIN", "SUPER_ADMIN"] : ["EMPLOYEE", "ADMIN"];
}

export default function SignUpForm() {
	const { user, isLoading } = useAuth();
	const router = useRouter();

	const [showPassword, setShowPassword] = useState(false);
	const [firstName, setFirstName] = useState("");
	const [lastName, setLastName] = useState("");
	const [email, setEmail] = useState("");
	const [password, setPassword] = useState("");
	const [confirmPassword, setConfirmPassword] = useState("");
	const [role, setRole] = useState<Role>("EMPLOYEE");
	const [error, setError] = useState<string | null>(null);
	const [successMessage, setSuccessMessage] = useState<string | null>(null);
	const [isSubmitting, setIsSubmitting] = useState(false);

	const isAuthorized = user?.role === "ADMIN" || user?.role === "SUPER_ADMIN";

	useEffect(() => {
		if (isLoading) return;
		if (!user) {
			router.replace("/signin");
		} else if (!isAuthorized) {
			router.replace("/");
		}
	}, [isLoading, user, isAuthorized, router]);

	async function handleSubmit(e: FormEvent<HTMLFormElement>) {
		e.preventDefault();
		setError(null);
		setSuccessMessage(null);

		if (password !== confirmPassword) {
			setError("Passwords do not match.");
			return;
		}

		setIsSubmitting(true);
		try {
			const created = await createUser({ firstName, lastName, email, password, role });
			setSuccessMessage(`Account created for ${created.email} (${ROLE_LABELS[created.role]}).`);
			setFirstName("");
			setLastName("");
			setEmail("");
			setPassword("");
			setConfirmPassword("");
			setRole("EMPLOYEE");
		} catch (err) {
			setError(getAuthErrorMessage(err));
		} finally {
			setIsSubmitting(false);
		}
	}

	if (isLoading || !user || !isAuthorized) {
		return (
			<div className="flex items-center justify-center min-h-screen">
				<p className="text-gray-500 dark:text-gray-400">Loading…</p>
			</div>
		);
	}

	return (
		<div className="flex flex-col flex-1 lg:w-1/2 w-full overflow-y-auto no-scrollbar">
			<div className="w-full max-w-md sm:pt-10 mx-auto mb-5">
				<Link
					href="/"
					className="inline-flex items-center text-sm text-gray-500 transition-colors hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300"
				>
					<ChevronLeftIcon />
					Back to dashboard
				</Link>
			</div>
			<div className="flex flex-col justify-center flex-1 w-full max-w-md mx-auto">
				<div>
					<div className="mb-5 sm:mb-8">
						<h1 className="mb-2 font-semibold text-gray-800 text-title-sm dark:text-white/90 sm:text-title-md">
							Add Staff Account
						</h1>
						<p className="text-sm text-gray-500 dark:text-gray-400">
							Create a login for a new employee or admin at your business.
						</p>
					</div>
					{error && (
						<div className="mb-5">
							<Alert variant="error" title="Could not create account" message={error} />
						</div>
					)}
					{successMessage && (
						<div className="mb-5">
							<Alert variant="success" title="Account created" message={successMessage} />
						</div>
					)}
					<form onSubmit={handleSubmit}>
						<div className="space-y-5">
							<div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
								<div className="sm:col-span-1">
									<Label>
										First Name<span className="text-error-500">*</span>
									</Label>
									<Input
										type="text"
										id="fname"
										name="fname"
										placeholder="Enter first name"
										defaultValue={firstName}
										onChange={(e) => setFirstName(e.target.value)}
									/>
								</div>
								<div className="sm:col-span-1">
									<Label>
										Last Name<span className="text-error-500">*</span>
									</Label>
									<Input
										type="text"
										id="lname"
										name="lname"
										placeholder="Enter last name"
										defaultValue={lastName}
										onChange={(e) => setLastName(e.target.value)}
									/>
								</div>
							</div>
							<div>
								<Label>
									Email<span className="text-error-500">*</span>
								</Label>
								<Input
									type="email"
									id="email"
									name="email"
									placeholder="colleague@example.com"
									defaultValue={email}
									onChange={(e) => setEmail(e.target.value)}
								/>
							</div>
							<div>
								<Label htmlFor="role">
									Role<span className="text-error-500">*</span>
								</Label>
								<select
									id="role"
									name="role"
									value={role}
									onChange={(e) => setRole(e.target.value as Role)}
									className="h-11 w-full rounded-lg border appearance-none px-4 py-2.5 text-sm shadow-theme-xs bg-transparent text-gray-800 border-gray-300 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 dark:border-gray-700 dark:bg-gray-900 dark:text-white/90 dark:focus:border-brand-800"
								>
									{grantableRoles(user.role).map((r) => (
										<option key={r} value={r}>
											{ROLE_LABELS[r]}
										</option>
									))}
								</select>
							</div>
							<div>
								<Label>
									Password<span className="text-error-500">*</span>
								</Label>
								<div className="relative">
									<Input
										placeholder="Enter a password"
										type={showPassword ? "text" : "password"}
										defaultValue={password}
										onChange={(e) => setPassword(e.target.value)}
									/>
									<span
										onClick={() => setShowPassword(!showPassword)}
										className="absolute z-30 -translate-y-1/2 cursor-pointer right-4 top-1/2"
									>
										{showPassword ? (
											<EyeIcon className="fill-gray-500 dark:fill-gray-400" />
										) : (
											<EyeCloseIcon className="fill-gray-500 dark:fill-gray-400" />
										)}
									</span>
								</div>
							</div>
							<div>
								<Label>
									Confirm Password<span className="text-error-500">*</span>
								</Label>
								<Input
									placeholder="Re-enter the password"
									type={showPassword ? "text" : "password"}
									defaultValue={confirmPassword}
									onChange={(e) => setConfirmPassword(e.target.value)}
								/>
							</div>
							<div>
								<Button className="w-full" size="sm" disabled={isSubmitting}>
									{isSubmitting ? "Creating account..." : "Create account"}
								</Button>
							</div>
						</div>
					</form>
				</div>
			</div>
		</div>
	);
}
