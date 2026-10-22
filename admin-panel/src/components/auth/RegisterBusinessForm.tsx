"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import Alert from "@/components/ui/alert/Alert";
import Button from "@/components/ui/button/Button";
import Field, { PasswordInput, SelectInput, TextareaInput, TextInput } from "@/components/ui/form/Field";
import { normalizeError } from "@/lib/api/errors";
import { CURRENCY_OPTIONS } from "@/lib/utils/money";
import { DEFAULT_TIME_ZONE, timeZoneOptions } from "@/lib/utils/timeZones";
import { useRegisterBusiness } from "@/lib/resources/registration/registration.hooks";
import type { RegistrationInput } from "@/lib/resources/registration/registration.types";
import {
	conflictField,
	registrationErrors,
	type RegistrationErrors,
} from "@/lib/resources/registration/registration.validation";
import AuthPanel from "./AuthPanel";

const BACK_TO_SIGN_IN = { href: "/signin", label: "Back to sign in" };

const EMPTY: RegistrationInput = {
	firstName: "",
	lastName: "",
	email: "",
	password: "",
	salonName: "",
	description: "",
	image: "",
	currency: "EUR",
	timezone: DEFAULT_TIME_ZONE,
	street: "",
	postalCode: "",
	city: "",
	country: "",
	phone: "",
};

/**
 * A salon owner registers their salon and their own account. The salon waits for a super admin to
 * approve it, and the owner can sign in only then, as the confirmation says.
 */
export default function RegisterBusinessForm() {
	const register = useRegisterBusiness();
	const [values, setValues] = useState<RegistrationInput>(EMPTY);
	const [confirmPassword, setConfirmPassword] = useState("");
	const [errors, setErrors] = useState<RegistrationErrors>({});
	const [failure, setFailure] = useState<string | null>(null);
	const [registered, setRegistered] = useState<RegistrationInput | null>(null);

	function set<K extends keyof RegistrationInput>(field: K, value: RegistrationInput[K]) {
		setValues((v) => ({ ...v, [field]: value }));
	}

	async function handleSubmit(e: FormEvent<HTMLFormElement>) {
		e.preventDefault();
		const found = registrationErrors(values, confirmPassword);
		setErrors(found);
		setFailure(null);
		if (Object.keys(found).length > 0) return;

		try {
			await register.mutateAsync(values);
			setRegistered(values);
		} catch (err) {
			const apiError = normalizeError(err);
			const field = apiError.kind === "conflict" ? conflictField(apiError.message) : null;
			if (field) setErrors({ [field]: apiError.message });
			else setFailure(apiError.message);
		}
	}

	if (registered) {
		return (
			<AuthPanel
				title="Your salon is registered"
				description={`We've let the platform team know about ${registered.salonName.trim()}.`}
				back={BACK_TO_SIGN_IN}
			>
				<p className="text-sm text-neutral-500 dark:text-neutral-400">
					Once they approve it, you can sign in as{" "}
					<strong className="text-ink">{registered.email.trim()}</strong> to set up your services,
					team and opening hours. Until then, signing in tells you it&apos;s waiting for approval.
				</p>
				<Link
					href="/signin"
					className="mt-6 inline-flex w-full items-center justify-center rounded-lg border border-border-strong px-4 py-3 text-sm font-medium text-ink transition-colors hover:bg-neutral-100 dark:hover:bg-white/5"
				>
					Go to sign in
				</Link>
			</AuthPanel>
		);
	}

	return (
		<AuthPanel
			title="Register your salon"
			description="Tell us about yourself and your salon. A platform admin reviews every new salon before it opens."
			back={BACK_TO_SIGN_IN}
			long
		>
			{failure && (
				<div className="mb-5">
					<Alert variant="error" title="Couldn't register your salon" message={failure} />
				</div>
			)}
			<form onSubmit={handleSubmit} noValidate className="flex flex-col gap-8">
				<fieldset className="flex flex-col gap-5">
					<legend className="mb-4 text-sm font-semibold text-ink">You</legend>
					<div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
						<Field label="First name" required error={errors.firstName}>
							{(p) => (
								<TextInput
									{...p}
									value={values.firstName}
									onChange={(e) => set("firstName", e.target.value)}
									autoComplete="given-name"
								/>
							)}
						</Field>
						<Field label="Last name" required error={errors.lastName}>
							{(p) => (
								<TextInput
									{...p}
									value={values.lastName}
									onChange={(e) => set("lastName", e.target.value)}
									autoComplete="family-name"
								/>
							)}
						</Field>
					</div>
					<Field
						label="Email"
						required
						error={errors.email}
						hint="You'll sign in with it once your salon is approved."
					>
						{(p) => (
							<TextInput
								{...p}
								type="email"
								value={values.email}
								onChange={(e) => set("email", e.target.value)}
								autoComplete="email"
							/>
						)}
					</Field>
					<div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
						<Field label="Password" required error={errors.password}>
							{(p) => (
								<PasswordInput
									{...p}
									value={values.password}
									onChange={(e) => set("password", e.target.value)}
									autoComplete="new-password"
								/>
							)}
						</Field>
						<Field label="Confirm password" required error={errors.confirmPassword}>
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
				</fieldset>

				<fieldset className="flex flex-col gap-5">
					<legend className="mb-4 text-sm font-semibold text-ink">Your salon</legend>
					<Field label="Name" required error={errors.salonName}>
						{(p) => (
							<TextInput
								{...p}
								value={values.salonName}
								onChange={(e) => set("salonName", e.target.value)}
								autoComplete="organization"
							/>
						)}
					</Field>
					<Field label="Description" error={errors.description}>
						{(p) => (
							<TextareaInput
								{...p}
								rows={3}
								value={values.description}
								onChange={(e) => set("description", e.target.value)}
								placeholder="What your salon offers, in a sentence or two."
							/>
						)}
					</Field>
					<Field label="Image URL" required error={errors.image} hint="A photo or logo, by its web address.">
						{(p) => (
							<TextInput
								{...p}
								type="url"
								value={values.image}
								onChange={(e) => set("image", e.target.value)}
								placeholder="https://"
							/>
						)}
					</Field>
					<div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
						<Field label="Currency" required>
							{(p) => (
								<SelectInput {...p} value={values.currency} onChange={(e) => set("currency", e.target.value)}>
									{CURRENCY_OPTIONS.map((c) => (
										<option key={c} value={c}>
											{c}
										</option>
									))}
								</SelectInput>
							)}
						</Field>
						<Field label="Time zone" required>
							{(p) => (
								<SelectInput {...p} value={values.timezone} onChange={(e) => set("timezone", e.target.value)}>
									{timeZoneOptions(values.timezone).map((zone) => (
										<option key={zone} value={zone}>
											{zone}
										</option>
									))}
								</SelectInput>
							)}
						</Field>
					</div>
					<Field label="Street" required error={errors.street}>
						{(p) => (
							<TextInput
								{...p}
								value={values.street}
								onChange={(e) => set("street", e.target.value)}
								autoComplete="address-line1"
							/>
						)}
					</Field>
					<div className="grid grid-cols-1 gap-5 sm:grid-cols-[2fr_3fr]">
						<Field label="Postal code" error={errors.postalCode}>
							{(p) => (
								<TextInput
									{...p}
									value={values.postalCode}
									onChange={(e) => set("postalCode", e.target.value)}
									autoComplete="postal-code"
								/>
							)}
						</Field>
						<Field label="City" required error={errors.city}>
							{(p) => (
								<TextInput
									{...p}
									value={values.city}
									onChange={(e) => set("city", e.target.value)}
									autoComplete="address-level2"
								/>
							)}
						</Field>
					</div>
					<Field label="Country" required error={errors.country}>
						{(p) => (
							<TextInput
								{...p}
								value={values.country}
								onChange={(e) => set("country", e.target.value)}
								autoComplete="country-name"
							/>
						)}
					</Field>
					<Field label="Phone" error={errors.phone}>
						{(p) => (
							<TextInput
								{...p}
								type="tel"
								value={values.phone}
								onChange={(e) => set("phone", e.target.value)}
								autoComplete="tel"
							/>
						)}
					</Field>
				</fieldset>

				<Button type="submit" className="w-full" size="sm" loading={register.isPending}>
					Register salon
				</Button>
			</form>
		</AuthPanel>
	);
}
