"use client";

import { useState, type FormEvent } from "react";
import { useParams } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { can } from "@/lib/auth/permissions";
import { useCustomer, useUpdateCustomer } from "@/lib/resources/customers/customers.hooks";
import { customerInputFrom } from "@/lib/resources/customers/customers.mappers";
import type { Customer } from "@/lib/resources/customers/customers.types";
import { getErrorMessage } from "@/lib/api/errors";
import Card from "@/components/ui/Card";
import EmptyState from "@/components/ui/EmptyState";
import Button from "@/components/ui/button/Button";
import Field, { TextareaInput } from "@/components/ui/form/Field";
import { Skeleton } from "@/components/ui/Skeleton";
import { useToast } from "@/components/ui/toast/ToastProvider";
import { DocsIcon } from "@/icons";

export default function CustomerNotesView() {
	const params = useParams<{ id: string }>();
	const id = Number(params.id);
	const { user } = useAuth();
	const { data: customer, isPending } = useCustomer(Number.isNaN(id) ? null : id);

	if (isPending || !customer) return <Skeleton className="h-64 rounded-card" />;

	// Employees can read notes; saving is a full-record PUT, which only an admin may do.
	return can(user, "customer:edit") ? (
		<NotesEditor customer={customer} />
	) : (
		<Card title="Notes">
			{customer.notes ? (
				<p className="whitespace-pre-line text-sm leading-relaxed text-ink">{customer.notes}</p>
			) : (
				<EmptyState
					className="border-0 bg-transparent py-8"
					icon={<DocsIcon className="size-6" />}
					title="No notes yet"
					description="An admin can add preferences, allergies and anything worth remembering before the next visit."
				/>
			)}
		</Card>
	);
}

/** Mounted once the customer has loaded, so the textarea initialises from the saved notes. */
function NotesEditor({ customer }: { customer: Customer }) {
	const { toast } = useToast();
	const update = useUpdateCustomer();
	const [notes, setNotes] = useState(customer.notes ?? "");
	const dirty = notes.trim() !== (customer.notes ?? "");

	async function handleSubmit(e: FormEvent<HTMLFormElement>) {
		e.preventDefault();
		try {
			// PUT replaces the whole record, so send every other field as it is now.
			await update.mutateAsync({
				businessId: customer.businessId,
				id: customer.id,
				input: customerInputFrom(customer, { notes }),
			});
			toast({ tone: "success", title: "Notes saved" });
		} catch (err) {
			toast({ tone: "error", title: "Couldn't save notes", description: getErrorMessage(err) });
		}
	}

	return (
		<form onSubmit={handleSubmit}>
			<Card
				title="Notes"
				description="Preferences, allergies and anything worth remembering before the next visit. Only your team sees these."
			>
				<div className="flex flex-col gap-4">
					<Field label="Notes" hint="Up to 5000 characters.">
						{(p) => (
							<TextareaInput
								{...p}
								value={notes}
								onChange={(e) => setNotes(e.target.value)}
								maxLength={5000}
								rows={8}
							/>
						)}
					</Field>
					<div className="flex justify-end">
						<Button type="submit" loading={update.isPending} disabled={!dirty}>
							Save notes
						</Button>
					</div>
				</div>
			</Card>
		</form>
	);
}
