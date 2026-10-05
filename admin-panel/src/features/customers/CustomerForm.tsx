"use client";

import { useState, type FormEvent } from "react";
import { useBusinesses } from "@/lib/resources/businesses/businesses.hooks";
import type { Customer, CustomerInput } from "@/lib/resources/customers/customers.types";
import Card from "@/components/ui/Card";
import Button from "@/components/ui/button/Button";
import Field, { SelectInput, TextInput, TextareaInput } from "@/components/ui/form/Field";
import Switch from "@/components/form/switch/Switch";

type Errors = Partial<Record<"firstName" | "lastName" | "email" | "businessId", string>>;

// Mirrors the backend's CustomerRequest limits so the form fails before the request does.
const NAME_MAX = 100;
const PHONE_MAX = 50;
const NOTES_MAX = 5000;

/**
 * Create and edit share this form because the backend uses one body for both: PUT
 * replaces every field. `initial` must therefore be the full current record on edit,
 * or saving would clear whatever the form didn't show.
 *
 * `pickBusiness` is for a SUPER_ADMIN creating a client: anyone else always adds to
 * their own salon, which the caller resolves.
 */
export default function CustomerForm({
	initial,
	pickBusiness = false,
	submitLabel,
	submitting,
	onSubmit,
	onCancel,
}: {
	initial?: Customer;
	pickBusiness?: boolean;
	submitLabel: string;
	submitting: boolean;
	onSubmit: (input: CustomerInput, businessId: number | null) => void | Promise<void>;
	onCancel: () => void;
}) {
	const { data: businesses, isPending: businessesPending } = useBusinesses();

	const [firstName, setFirstName] = useState(initial?.firstName ?? "");
	const [lastName, setLastName] = useState(initial?.lastName ?? "");
	const [email, setEmail] = useState(initial?.email ?? "");
	const [phone, setPhone] = useState(initial?.phone ?? "");
	const [notes, setNotes] = useState(initial?.notes ?? "");
	const [marketingConsent, setMarketingConsent] = useState(initial?.marketingConsent ?? false);
	const [businessId, setBusinessId] = useState("");
	const [errors, setErrors] = useState<Errors>({});

	function validate(): Errors {
		const found: Errors = {};
		if (!firstName.trim()) found.firstName = "First name is required.";
		else if (firstName.trim().length > NAME_MAX) found.firstName = `Use at most ${NAME_MAX} characters.`;
		if (!lastName.trim()) found.lastName = "Last name is required.";
		else if (lastName.trim().length > NAME_MAX) found.lastName = `Use at most ${NAME_MAX} characters.`;
		if (email.trim() && !/^\S+@\S+\.\S+$/.test(email.trim())) found.email = "Enter a valid email address.";
		if (pickBusiness && !businessId) found.businessId = "Choose which salon this client belongs to.";
		return found;
	}

	async function handleSubmit(e: FormEvent<HTMLFormElement>) {
		e.preventDefault();
		const found = validate();
		setErrors(found);
		if (Object.keys(found).length > 0) return;

		await onSubmit(
			{ firstName, lastName, email, phone, notes, marketingConsent },
			pickBusiness ? Number(businessId) : null,
		);
	}

	return (
		<form onSubmit={handleSubmit} className="flex flex-col gap-4 md:gap-6">
			<Card title="Client details">
				<div className="flex flex-col gap-5">
					<div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
						<Field label="First name" required error={errors.firstName}>
							{(p) => (
								<TextInput
									{...p}
									value={firstName}
									onChange={(e) => setFirstName(e.target.value)}
									maxLength={NAME_MAX}
									autoComplete="off"
								/>
							)}
						</Field>
						<Field label="Last name" required error={errors.lastName}>
							{(p) => (
								<TextInput
									{...p}
									value={lastName}
									onChange={(e) => setLastName(e.target.value)}
									maxLength={NAME_MAX}
									autoComplete="off"
								/>
							)}
						</Field>
					</div>

					<div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
						<Field label="Email" hint="Optional." error={errors.email}>
							{(p) => (
								<TextInput
									{...p}
									type="email"
									value={email}
									onChange={(e) => setEmail(e.target.value)}
									autoComplete="off"
								/>
							)}
						</Field>
						<Field label="Phone" hint="Optional.">
							{(p) => (
								<TextInput
									{...p}
									type="tel"
									value={phone}
									onChange={(e) => setPhone(e.target.value)}
									maxLength={PHONE_MAX}
									autoComplete="off"
								/>
							)}
						</Field>
					</div>

					{pickBusiness && (
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

					<div className="flex items-center justify-between rounded-lg border border-border-default px-4 py-3">
						<div>
							<p className="text-sm font-medium text-ink">Marketing consent</p>
							<p className="text-sm text-ink-muted">
								Only contact this client with offers and newsletters if they agreed to it.
							</p>
						</div>
						<Switch label="" defaultChecked={marketingConsent} onChange={setMarketingConsent} />
					</div>
				</div>
			</Card>

			<Card title="Notes" description="Preferences, allergies and anything worth remembering. Only your team sees these.">
				<Field label="Notes" hint={`Optional, up to ${NOTES_MAX} characters.`}>
					{(p) => (
						<TextareaInput
							{...p}
							value={notes}
							onChange={(e) => setNotes(e.target.value)}
							maxLength={NOTES_MAX}
							rows={5}
						/>
					)}
				</Field>
			</Card>

			<div className="flex justify-end gap-3">
				<Button type="button" variant="outline" onClick={onCancel} disabled={submitting}>
					Cancel
				</Button>
				<Button type="submit" loading={submitting}>
					{submitLabel}
				</Button>
			</div>
		</form>
	);
}
