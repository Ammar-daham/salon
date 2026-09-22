"use client";
import Input from "@/components/form/input/InputField";
import Label from "@/components/form/Label";
import Alert from "@/components/ui/alert/Alert";
import Button from "@/components/ui/button/Button";
import { useAuth } from "@/context/AuthContext";
import { getErrorMessage } from "@/lib/api/errors";
import { createUser } from "@/lib/resources/users/users.api";
import { ROLE_LABELS, type Role } from "@/lib/resources/auth/auth.types";
import { grantableRoles } from "@/lib/auth/permissions";
import type { Business } from "@/lib/resources/businesses/businesses.types";
import { businessesRepository } from "@/lib/resources/businesses/businesses.api";
import { ChevronLeftIcon, EyeCloseIcon, EyeIcon } from "@/icons";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";

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
	// A super admin isn't tied to one business, so they must pick which business the new
	// account belongs to. A regular admin's own business is used implicitly by the backend.
	const mustPickBusiness = user?.role === "SUPER_ADMIN";

	const [businesses, setBusinesses] = useState<Business[]>([]);
	const [businessId, setBusinessId] = useState<number | "">("");
	const [isLoadingBusinesses, setIsLoadingBusinesses] = useState(false);

	useEffect(() => {
		if (isLoading) return;
		if (!user) {
			router.replace("/signin");
		} else if (!isAuthorized) {
			router.replace("/");
		}
	}, [isLoading, user, isAuthorized, router]);

	useEffect(() => {
		if (!mustPickBusiness) return;
		setIsLoadingBusinesses(true);
		businessesRepository.list()
			.then(setBusinesses)
			.catch((err) => setError(getErrorMessage(err)))
			.finally(() => setIsLoadingBusinesses(false));
	}, [mustPickBusiness]);

	async function handleSubmit(e: FormEvent<HTMLFormElement>) {
		e.preventDefault();
		setError(null);
		setSuccessMessage(null);

		if (password !== confirmPassword) {
			setError("Passwords do not match.");
			return;
		}
		if (mustPickBusiness && businessId === "") {
			setError("Please select which business this account belongs to.");
			return;
		}

		setIsSubmitting(true);
		try {
			const created = await createUser({
				firstName,
				lastName,
				email,
				password,
				role,
				...(mustPickBusiness ? { businessId: businessId as number } : {}),
			});
			setSuccessMessage(`Account created for ${created.email} (${ROLE_LABELS[created.role]}).`);
			setFirstName("");
			setLastName("");
			setEmail("");
			setPassword("");
			setConfirmPassword("");
			setRole("EMPLOYEE");
			setBusinessId("");
		} catch (err) {
			setError(getErrorMessage(err));
		} finally {
			setIsSubmitting(false);
		}
	}

	if (isLoading || !user || !isAuthorized) {
		return (
			<div className="flex items-center justify-center min-h-screen">
				<p className="text-neutral-500 dark:text-neutral-400">Loading…</p>
			</div>
		);
	}

	return (
		<div className="flex flex-col flex-1 lg:w-1/2 w-full overflow-y-auto no-scrollbar">
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
							Add Staff Account
						</h1>
						<p className="text-sm text-neutral-500 dark:text-neutral-400">
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
									className="h-11 w-full rounded-lg border appearance-none px-4 py-2.5 text-sm shadow-xs bg-transparent text-neutral-800 border-neutral-300 focus:border-primary-300 focus:outline-hidden focus:ring-3 focus:ring-primary-500/10 dark:border-neutral-700 dark:bg-neutral-900 dark:text-white/90 dark:focus:border-primary-800"
								>
									{grantableRoles(user).map((r) => (
										<option key={r} value={r}>
											{ROLE_LABELS[r]}
										</option>
									))}
								</select>
							</div>
							{mustPickBusiness && (
								<div>
									<Label htmlFor="business">
										Business<span className="text-error-500">*</span>
									</Label>
									<select
										id="business"
										name="business"
										value={businessId}
										onChange={(e) => setBusinessId(e.target.value ? Number(e.target.value) : "")}
										disabled={isLoadingBusinesses}
										className="h-11 w-full rounded-lg border appearance-none px-4 py-2.5 text-sm shadow-xs bg-transparent text-neutral-800 border-neutral-300 focus:border-primary-300 focus:outline-hidden focus:ring-3 focus:ring-primary-500/10 dark:border-neutral-700 dark:bg-neutral-900 dark:text-white/90 dark:focus:border-primary-800"
									>
										<option value="" disabled>
											{isLoadingBusinesses ? "Loading businesses…" : "Select a business"}
										</option>
										{businesses.map((b) => (
											<option key={b.id} value={b.id}>
												{b.name}
											</option>
										))}
									</select>
								</div>
							)}
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
											<EyeIcon className="fill-neutral-500 dark:fill-neutral-400" />
										) : (
											<EyeCloseIcon className="fill-neutral-500 dark:fill-neutral-400" />
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
								<Button type="submit" className="w-full" size="sm" loading={isSubmitting}>
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
