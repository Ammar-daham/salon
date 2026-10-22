"use client";

import React, { useState, type FormEvent } from "react";
import Field, { SelectInput, TextInput, TextareaInput } from "@/components/ui/form/Field";
import Button from "@/components/ui/button/Button";
import Card from "@/components/ui/Card";
import {
	BUSINESS_STATUSES,
	BUSINESS_STATUS_LABELS,
	type Business,
	type BusinessInput,
	type BusinessStatus,
} from "@/lib/resources/businesses/businesses.types";
import { addressErrors, hasAddress } from "@/lib/resources/businesses/businesses.validation";
import { CURRENCY_OPTIONS } from "@/lib/utils/money";
import { DEFAULT_TIME_ZONE, timeZoneOptions } from "@/lib/utils/timeZones";

export interface BusinessFormValues {
	name: string;
	description: string;
	image: string;
	status: BusinessStatus;
	currency: string;
	timezone: string;
	/** Create only: the salon's address and phone, both optional. */
	street: string;
	postalCode: string;
	city: string;
	country: string;
	phone: string;
}

interface BusinessFormProps {
	mode: "create" | "edit";
	initial?: Business;
	/** Only SUPER_ADMIN may choose a status, and only while creating. */
	allowStatus?: boolean;
	submitting?: boolean;
	onSubmit: (input: BusinessInput) => void | Promise<void>;
	onCancel: () => void;
}

type Errors = Partial<Record<keyof BusinessFormValues, string>>;

function addressOf(values: BusinessFormValues) {
	return { street: values.street, postalCode: values.postalCode, city: values.city, country: values.country };
}

function validate(values: BusinessFormValues, creating: boolean): Errors {
	const errors: Errors = creating ? addressErrors(addressOf(values)) : {};
	if (creating && values.phone.trim().length > 255) errors.phone = "Use at most 255 characters.";
	if (!values.name.trim()) errors.name = "Give the salon a name.";
	else if (values.name.trim().length > 100) errors.name = "Keep the name under 100 characters.";
	// image is NOT NULL in the database and there is no upload endpoint, so the
	// client has to supply something. An empty value is a guaranteed 400.
	if (!values.image.trim()) errors.image = "An image URL is required.";
	return errors;
}

export default function BusinessForm({
	mode,
	initial,
	allowStatus = false,
	submitting = false,
	onSubmit,
	onCancel,
}: BusinessFormProps) {
	const [values, setValues] = useState<BusinessFormValues>({
		name: initial?.name ?? "",
		description: initial?.description ?? "",
		image: initial?.image ?? "",
		status: initial?.status ?? "PENDING",
		currency: initial?.currency ?? "EUR",
		timezone: initial?.timezone ?? DEFAULT_TIME_ZONE,
		street: "",
		postalCode: "",
		city: "",
		country: "",
		phone: "",
	});
	const [errors, setErrors] = useState<Errors>({});

	function set<K extends keyof BusinessFormValues>(key: K, value: BusinessFormValues[K]) {
		setValues((v) => ({ ...v, [key]: value }));
		setErrors((e) => ({ ...e, [key]: undefined }));
	}

	async function handleSubmit(e: FormEvent<HTMLFormElement>) {
		e.preventDefault();
		const creating = mode === "create";
		const found = validate(values, creating);
		setErrors(found);
		if (Object.keys(found).length > 0) return;

		await onSubmit({
			name: values.name.trim(),
			description: values.description.trim() || null,
			image: values.image.trim(),
			// Not sent to the API either way: see toBusinessRequest.
			...(allowStatus ? { status: values.status } : {}),
			currency: values.currency,
			timezone: values.timezone,
			// Only a new salon's: the update leaves its addresses and contacts alone.
			...(creating
				? {
						address: hasAddress(addressOf(values)) ? addressOf(values) : null,
						phone: values.phone.trim() || null,
					}
				: {}),
		});
	}

	return (
		<form onSubmit={handleSubmit} className="flex flex-col gap-4 md:gap-6">
			<Card title="Salon details">
				<div className="flex flex-col gap-5">
					<Field label="Name" required error={errors.name}>
						{(p) => (
							<TextInput
								{...p}
								value={values.name}
								onChange={(e) => set("name", e.target.value)}
								placeholder="Lumière Beauty Studio"
								autoComplete="organization"
							/>
						)}
					</Field>

					<Field
						label="Description"
						hint="A short line shown on the salon's profile."
						error={errors.description}
					>
						{(p) => (
							<TextareaInput
								{...p}
								value={values.description}
								onChange={(e) => set("description", e.target.value)}
								placeholder="Colour specialists and bridal styling in the heart of Helsinki."
							/>
						)}
					</Field>

					<Field
						label="Image URL"
						required
						hint="There's no file upload yet — paste a link to an image."
						error={errors.image}
					>
						{(p) => (
							<TextInput
								{...p}
								value={values.image}
								onChange={(e) => set("image", e.target.value)}
								placeholder="https://example.com/salon.jpg"
								inputMode="url"
							/>
						)}
					</Field>

					{values.image.trim() && (
						<div className="flex items-center gap-3">
							{/* eslint-disable-next-line @next/next/no-img-element -- arbitrary
							    client-supplied URL, see BusinessListView. */}
							<img
								src={values.image}
								alt=""
								className="size-16 rounded-lg border border-border-default object-cover"
								onError={(e) => {
									e.currentTarget.style.visibility = "hidden";
								}}
							/>
							<span className="text-sm text-ink-subtle">Preview</span>
						</div>
					)}

					<Field
						label="Currency"
						hint="Every service price at this salon is in this currency. Changing it doesn't convert existing prices."
					>
						{(p) => (
							<SelectInput
								{...p}
								value={values.currency}
								onChange={(e) => set("currency", e.target.value)}
							>
								{[...new Set([values.currency, ...CURRENCY_OPTIONS])].map((c) => (
									<option key={c} value={c}>
										{c}
									</option>
								))}
							</SelectInput>
						)}
					</Field>

					<Field
						label="Time zone"
						hint="Opening hours, working hours and appointments are on this clock. Changing it keeps appointments at the same moment, so their times shift."
					>
						{(p) => (
							<SelectInput
								{...p}
								value={values.timezone}
								onChange={(e) => set("timezone", e.target.value)}
							>
								{timeZoneOptions(values.timezone).map((zone) => (
									<option key={zone} value={zone}>
										{zone.replaceAll("_", " ")}
									</option>
								))}
							</SelectInput>
						)}
					</Field>

					{allowStatus && (
						<Field
							label="Status"
							hint="Sets the salon's initial state. Approving or suspending it later is a separate platform action."
						>
							{(p) => (
								<SelectInput
									{...p}
									value={values.status}
									onChange={(e) => set("status", e.target.value as BusinessStatus)}
								>
									{BUSINESS_STATUSES.map((s) => (
										<option key={s} value={s}>
											{BUSINESS_STATUS_LABELS[s]}
										</option>
									))}
								</SelectInput>
							)}
						</Field>
					)}
				</div>
			</Card>

			{mode === "create" && (
				<Card
					title="Address and phone"
					description="Optional. Customers see them on the salon's listing."
				>
					<div className="flex flex-col gap-5">
						<Field label="Street" error={errors.street}>
							{(p) => (
								<TextInput
									{...p}
									value={values.street}
									onChange={(e) => set("street", e.target.value)}
									autoComplete="address-line1"
								/>
							)}
						</Field>
						<div className="grid grid-cols-1 gap-5 sm:grid-cols-3">
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
							<Field label="City" error={errors.city}>
								{(p) => (
									<TextInput
										{...p}
										value={values.city}
										onChange={(e) => set("city", e.target.value)}
										autoComplete="address-level2"
									/>
								)}
							</Field>
							<Field label="Country" error={errors.country}>
								{(p) => (
									<TextInput
										{...p}
										value={values.country}
										onChange={(e) => set("country", e.target.value)}
										autoComplete="country-name"
									/>
								)}
							</Field>
						</div>
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
					</div>
				</Card>
			)}

			<div className="flex justify-end gap-3">
				<Button type="button" variant="outline" onClick={onCancel} disabled={submitting}>
					Cancel
				</Button>
				<Button type="submit" loading={submitting}>
					{mode === "create" ? "Create salon" : "Save changes"}
				</Button>
			</div>
		</form>
	);
}
