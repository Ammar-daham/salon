"use client";

import { useState, type FormEvent } from "react";
import { useAuth } from "@/context/AuthContext";
import { grantableRoles } from "@/lib/auth/permissions";
import { resolveBusinessScope } from "@/lib/auth/scope";
import { useBusinesses } from "@/lib/resources/businesses/businesses.hooks";
import { ROLE_LABELS, type Role } from "@/lib/resources/auth/auth.types";
import { newPasswordErrors } from "@/lib/resources/auth/auth.validation";
import type { CreateUserInput, UpdateUserInput, User } from "@/lib/resources/users/users.types";

import Card from "@/components/ui/Card";
import Button from "@/components/ui/button/Button";
import Field, { PasswordInput, SelectInput, TextInput } from "@/components/ui/form/Field";

type Errors = Partial<Record<string, string>>;

interface UserFormProps {
	mode: "create" | "edit";
	initial?: User;
	/** Restricts the role options, e.g. the Add-employee flow. */
	allowedRoles?: Role[];
	submitting?: boolean;
	onSubmitCreate?: (input: CreateUserInput) => Promise<void>;
	onSubmitUpdate?: (input: UpdateUserInput) => Promise<void>;
	onCancel: () => void;
	/**
	 * Extra fields rendered inside this same form, between the account and
	 * password sections - e.g. the Add-employee flow's job title and hire date,
	 * which belong to a staff record this form knows nothing about. Validating
	 * and submitting them is the caller's job: this form only provides the slot.
	 */
	extraSection?: React.ReactNode;
}

/**
 * Mirrors the backend's user rules exactly (UserService.addUser):
 *  - an ADMIN creating a SUPER_ADMIN is a 403, so that option is never offered
 *  - a SUPER_ADMIN caller MUST supply businessId or it's a 400
 *  - any other caller has their own businessId substituted server-side
 *  - email + password are required for every role except CUSTOMER
 *
 * On edit the backend only writes first_name, last_name and role — email can't
 * be changed at all, and only the account holder can change their password, in
 * Settings. The form shows email as read-only rather than pretending otherwise.
 */
export default function UserForm({
	mode,
	initial,
	allowedRoles,
	submitting = false,
	onSubmitCreate,
	onSubmitUpdate,
	onCancel,
	extraSection,
}: UserFormProps) {
	const { user: currentUser } = useAuth();
	const scope = resolveBusinessScope(currentUser);
	const isCreate = mode === "create";

	const grantable = grantableRoles(currentUser).filter(
		(r) => !allowedRoles || allowedRoles.includes(r),
	);

	// Only a platform admin picks a salon; everyone else is scoped server-side.
	const mustPickBusiness = isCreate && scope.kind === "platform";
	const { data: businesses, isPending: businessesPending } = useBusinesses();

	const [firstName, setFirstName] = useState(initial?.firstName ?? "");
	const [lastName, setLastName] = useState(initial?.lastName ?? "");
	const [email, setEmail] = useState(initial?.email ?? "");
	const [password, setPassword] = useState("");
	const [confirmPassword, setConfirmPassword] = useState("");
	const [role, setRole] = useState<Role>(initial?.role ?? grantable[0] ?? "EMPLOYEE");
	const [businessId, setBusinessId] = useState("");
	const [errors, setErrors] = useState<Errors>({});

	const needsCredentials = role !== "CUSTOMER";

	function validate(): Errors {
		const found: Errors = {};
		if (!firstName.trim()) found.firstName = "First name is required.";
		if (!lastName.trim()) found.lastName = "Last name is required.";

		if (isCreate && needsCredentials) {
			if (!email.trim()) found.email = "Email is required for accounts that sign in.";
			else if (!/^\S+@\S+\.\S+$/.test(email.trim())) found.email = "Enter a valid email address.";

			Object.assign(found, newPasswordErrors(password, confirmPassword));
		}

		if (mustPickBusiness && !businessId) {
			// The backend returns 400 when a SUPER_ADMIN omits businessId.
			found.businessId = "Choose which salon this account belongs to.";
		}
		return found;
	}

	async function handleSubmit(e: FormEvent<HTMLFormElement>) {
		e.preventDefault();
		const found = validate();
		setErrors(found);
		if (Object.keys(found).length > 0) return;

		if (isCreate) {
			await onSubmitCreate?.({
				firstName: firstName.trim(),
				lastName: lastName.trim(),
				email: email.trim(),
				password,
				role,
				...(mustPickBusiness ? { businessId: Number(businessId) } : {}),
			});
		} else {
			await onSubmitUpdate?.({
				firstName: firstName.trim(),
				lastName: lastName.trim(),
				// Always sent: UserDataAccessService NPEs into a 400 when role is null.
				role,
			});
		}
	}

	return (
		<form onSubmit={handleSubmit} className="flex flex-col gap-4 md:gap-6">
			<Card title="Account details">
				<div className="flex flex-col gap-5">
					<div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
						<Field label="First name" required error={errors.firstName}>
							{(p) => (
								<TextInput
									{...p}
									value={firstName}
									onChange={(e) => setFirstName(e.target.value)}
									autoComplete="given-name"
								/>
							)}
						</Field>
						<Field label="Last name" required error={errors.lastName}>
							{(p) => (
								<TextInput
									{...p}
									value={lastName}
									onChange={(e) => setLastName(e.target.value)}
									autoComplete="family-name"
								/>
							)}
						</Field>
					</div>

					<Field
						label="Email"
						required={isCreate && needsCredentials}
						error={errors.email}
						hint={!isCreate ? "Email can't be changed after the account is created." : undefined}
					>
						{(p) => (
							<TextInput
								{...p}
								type="email"
								value={email}
								onChange={(e) => setEmail(e.target.value)}
								readOnly={!isCreate}
								disabled={!isCreate}
								autoComplete="email"
								placeholder="name@salon.test"
							/>
						)}
					</Field>

					<Field label="Role" required error={errors.role}>
						{(p) => (
							<SelectInput {...p} value={role} onChange={(e) => setRole(e.target.value as Role)}>
								{grantable.map((r) => (
									<option key={r} value={r}>
										{ROLE_LABELS[r]}
									</option>
								))}
							</SelectInput>
						)}
					</Field>

					{mustPickBusiness && (
						<Field label="Salon" required error={errors.businessId}>
							{(p) => (
								<SelectInput
									{...p}
									value={businessId}
									onChange={(e) => setBusinessId(e.target.value)}
									disabled={businessesPending}
								>
									<option value="">
										{businessesPending ? "Loading salons…" : "Select a salon…"}
									</option>
									{(businesses ?? []).map((b) => (
										<option key={b.id} value={b.id}>
											{b.name}
										</option>
									))}
								</SelectInput>
							)}
						</Field>
					)}
				</div>
			</Card>

			{extraSection}

			{isCreate && needsCredentials && (
				<Card
					title="Password"
					description="The account holder can change it later in Settings."
				>
					<div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
						<Field label="Password" required error={errors.password}>
							{(p) => (
								<PasswordInput
									{...p}
									value={password}
									onChange={(e) => setPassword(e.target.value)}
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
				</Card>
			)}

			<div className="flex justify-end gap-3">
				<Button type="button" variant="outline" onClick={onCancel} disabled={submitting}>
					Cancel
				</Button>
				<Button type="submit" loading={submitting}>
					{isCreate ? "Create account" : "Save changes"}
				</Button>
			</div>
		</form>
	);
}
