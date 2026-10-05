"use client";

import { useParams, useRouter } from "next/navigation";
import { useCustomer, useUpdateCustomer } from "@/lib/resources/customers/customers.hooks";
import type { CustomerInput } from "@/lib/resources/customers/customers.types";
import { getErrorMessage } from "@/lib/api/errors";
import { Skeleton } from "@/components/ui/Skeleton";
import { useToast } from "@/components/ui/toast/ToastProvider";
import CustomerForm from "./CustomerForm";

export default function CustomerEditView() {
	const params = useParams<{ id: string }>();
	const id = Number(params.id);
	const router = useRouter();
	const { toast } = useToast();

	const { data: customer, isPending } = useCustomer(Number.isNaN(id) ? null : id);
	const update = useUpdateCustomer();

	// The form initialises its fields from `customer` at mount, so only mount it once loaded.
	if (isPending || !customer) return <Skeleton className="h-96 rounded-card" />;

	async function handleSubmit(input: CustomerInput) {
		try {
			await update.mutateAsync({ businessId: customer!.businessId, id, input });
			toast({ tone: "success", title: "Changes saved" });
			router.push(`/customers/${id}`);
		} catch (err) {
			toast({ tone: "error", title: "Couldn't save changes", description: getErrorMessage(err) });
		}
	}

	return (
		<div className="max-w-3xl">
			<CustomerForm
				initial={customer}
				submitLabel="Save changes"
				submitting={update.isPending}
				onSubmit={handleSubmit}
				onCancel={() => router.push(`/customers/${id}`)}
			/>
		</div>
	);
}
