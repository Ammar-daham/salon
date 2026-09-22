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

export interface BusinessFormValues {
	name: string;
	description: string;
	image: string;
	status: BusinessStatus;
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

function validate(values: BusinessFormValues): Errors {
	const errors: Errors = {};
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
	});
	const [errors, setErrors] = useState<Errors>({});

	function set<K extends keyof BusinessFormValues>(key: K, value: BusinessFormValues[K]) {
		setValues((v) => ({ ...v, [key]: value }));
		setErrors((e) => ({ ...e, [key]: undefined }));
	}

	async function handleSubmit(e: FormEvent<HTMLFormElement>) {
		e.preventDefault();
		const found = validate(values);
		setErrors(found);
		if (Object.keys(found).length > 0) return;

		await onSubmit({
			name: values.name.trim(),
			description: values.description.trim() || null,
			image: values.image.trim(),
			// Omitted on edit: the update SQL COALESCEs status, and the control
			// isn't offered there anyway.
			...(allowStatus ? { status: values.status } : {}),
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
				<Card title="Address and contacts">
					<p className="text-sm text-ink-muted">
						Addresses and contacts can&apos;t be added through the API yet — there are no
						endpoints for creating them, and a salon&apos;s set is fixed once it exists.
						Add them directly in the database for now.
					</p>
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
