"use client";

import { useState, type FormEvent } from "react";
import { Modal } from "@/components/ui/modal";
import Button from "@/components/ui/button/Button";
import Field, { SelectInput, TextInput, TextareaInput } from "@/components/ui/form/Field";
import Switch from "@/components/form/switch/Switch";
import { isDuplicateName } from "@/lib/resources/services/services.api";
import type {
	SalonService,
	SalonServiceInput,
} from "@/lib/resources/businesses/businesses.types";
import type { Id } from "@/lib/api/types";

/**
 * Mount this only while the dialog is open — `{open && <ServiceFormModal …/>}`.
 *
 * Field state is initialised from `initial` at mount, and the `Switch` primitive
 * is uncontrolled (it latches `defaultChecked` on mount and never re-reads it).
 * Keeping the component mounted across opens would therefore show the previously
 * edited row's toggle state. A fresh mount per open is the whole reset mechanism.
 */
interface ServiceFormModalProps {
	isOpen: boolean;
	onClose: () => void;
	onSubmit: (input: SalonServiceInput, businessId: Id) => Promise<void>;
	submitting?: boolean;
	initial?: SalonService | null;
	/** Used for the duplicate-name guard. */
	existing: SalonService[];
	/** The salon this service belongs to. Fixed when editing, or when the viewer
	 *  is scoped to a single salon. */
	businessId?: Id | null;
	/** Offered only when creating as a platform admin, who has no salon of their
	 *  own to default to. */
	businessOptions?: { id: Id; name: string }[];
}

type Errors = Partial<Record<"name" | "durationMinutes" | "price" | "businessId", string>>;

export default function ServiceFormModal({
	isOpen,
	onClose,
	onSubmit,
	submitting = false,
	initial = null,
	existing,
	businessId = null,
	businessOptions,
}: ServiceFormModalProps) {
	const isEdit = initial != null;

	const [name, setName] = useState(initial?.name ?? "");
	const [description, setDescription] = useState(initial?.description ?? "");
	const [duration, setDuration] = useState(String(initial?.durationMinutes ?? 60));
	const [price, setPrice] = useState(String(initial?.price ?? ""));
	const [isActive, setIsActive] = useState(initial?.isActive ?? true);
	const [targetBusiness, setTargetBusiness] = useState<string>(
		businessId != null ? String(businessId) : "",
	);
	const [errors, setErrors] = useState<Errors>({});

	const needsBusinessChoice = !isEdit && businessId == null && (businessOptions?.length ?? 0) > 0;

	function validate(): Errors {
		const found: Errors = {};
		if (needsBusinessChoice && !targetBusiness) found.businessId = "Choose which salon this belongs to.";
		if (!name.trim()) found.name = "Give the service a name.";
		else if (isDuplicateName(existing, name, initial?.id)) {
			// The backend has no unique constraint on service names and its
			// duplicate-key catch is unreachable — posting twice silently creates
			// two rows. This guard is the only protection.
			found.name = "A service with this name already exists at this salon.";
		}

		const mins = Number(duration);
		if (!Number.isFinite(mins) || mins <= 0) found.durationMinutes = "Enter a duration in minutes.";
		else if (!Number.isInteger(mins)) found.durationMinutes = "Duration must be whole minutes.";

		const amount = Number(price);
		if (price.trim() === "" || !Number.isFinite(amount) || amount < 0) {
			found.price = "Enter a price (0 is allowed for free services).";
		} else if (Math.round(amount * 100) / 100 !== amount) {
			// The column is DECIMAL(10,2).
			found.price = "Price can have at most two decimal places.";
		}
		return found;
	}

	async function handleSubmit(e: FormEvent<HTMLFormElement>) {
		e.preventDefault();
		const found = validate();
		setErrors(found);
		if (Object.keys(found).length > 0) return;

		await onSubmit(
			{
				name: name.trim(),
				description: description.trim() || null,
				durationMinutes: Number(duration),
				price: Number(price),
				isActive,
			},
			Number(targetBusiness),
		);
	}

	return (
		<Modal isOpen={isOpen} onClose={onClose} ariaLabel={isEdit ? "Edit service" : "New service"} className="max-w-lg">
			<form onSubmit={handleSubmit}>
				<div className="border-b border-border-default px-6 py-5">
					<h2 className="text-base font-semibold text-ink">
						{isEdit ? "Edit service" : "New service"}
					</h2>
					<p className="mt-1 text-sm text-ink-muted">
						{isEdit ? "Update the details clients see when booking." : "Add a treatment to the menu."}
					</p>
				</div>

				<div className="flex flex-col gap-5 px-6 py-5">
					{needsBusinessChoice && (
						<Field label="Salon" required error={errors.businessId}>
							{(p) => (
								<SelectInput
									{...p}
									value={targetBusiness}
									onChange={(e) => setTargetBusiness(e.target.value)}
								>
									<option value="">Select a salon…</option>
									{businessOptions!.map((b) => (
										<option key={b.id} value={b.id}>
											{b.name}
										</option>
									))}
								</SelectInput>
							)}
						</Field>
					)}

					<Field label="Name" required error={errors.name}>
						{(p) => (
							<TextInput
								{...p}
								value={name}
								onChange={(e) => setName(e.target.value)}
								placeholder="Haircut & style"
							/>
						)}
					</Field>

					<Field label="Description" error={undefined}>
						{(p) => (
							<TextareaInput
								{...p}
								rows={3}
								value={description}
								onChange={(e) => setDescription(e.target.value)}
								placeholder="Consultation, cut and blow-dry finish."
							/>
						)}
					</Field>

					<div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
						<Field label="Duration (minutes)" required error={errors.durationMinutes}>
							{(p) => (
								<TextInput
									{...p}
									type="number"
									min={1}
									step={5}
									value={duration}
									onChange={(e) => setDuration(e.target.value)}
								/>
							)}
						</Field>

						<Field label="Price (EUR)" required error={errors.price}>
							{(p) => (
								<TextInput
									{...p}
									type="number"
									min={0}
									step="0.01"
									value={price}
									onChange={(e) => setPrice(e.target.value)}
									placeholder="65.00"
								/>
							)}
						</Field>
					</div>

					<div className="flex items-center justify-between rounded-lg border border-border-default px-4 py-3">
						<div>
							<p className="text-sm font-medium text-ink">Bookable</p>
							<p className="text-sm text-ink-muted">Inactive services stay on file but can&apos;t be booked.</p>
						</div>
						<Switch
							label=""
							defaultChecked={isActive}
							onChange={setIsActive}
						/>
					</div>
				</div>

				<div className="flex justify-end gap-3 border-t border-border-default px-6 py-4">
					<Button type="button" variant="outline" onClick={onClose} disabled={submitting}>
						Cancel
					</Button>
					<Button type="submit" loading={submitting}>
						{isEdit ? "Save changes" : "Add service"}
					</Button>
				</div>
			</form>
		</Modal>
	);
}
