"use client";

import { useParams, useRouter } from "next/navigation";
import { useBusiness, useUpdateBusiness } from "@/lib/resources/businesses/businesses.hooks";
import type { BusinessInput } from "@/lib/resources/businesses/businesses.types";
import { getErrorMessage } from "@/lib/api/errors";
import { useToast } from "@/components/ui/toast/ToastProvider";
import { Skeleton } from "@/components/ui/Skeleton";
import Card from "@/components/ui/Card";
import BusinessForm from "./BusinessForm";

export default function BusinessSettingsView() {
	const params = useParams<{ id: string }>();
	const id = Number(params.id);
	const router = useRouter();
	const { toast } = useToast();

	const { data: business, isPending } = useBusiness(Number.isNaN(id) ? null : id);
	const update = useUpdateBusiness();

	async function handleSubmit(input: BusinessInput) {
		try {
			await update.mutateAsync({ id, input });
			toast({ tone: "success", title: "Changes saved" });
		} catch (err) {
			toast({ tone: "error", title: "Couldn't save changes", description: getErrorMessage(err) });
		}
	}

	if (isPending || !business) {
		return <Skeleton className="h-96 rounded-card" />;
	}

	return (
		<div className="max-w-3xl">
			<BusinessForm
				mode="edit"
				initial={business}
				/* Status is deliberately not offered here: approve/reject/suspend is a
				   platform decision with its own endpoint, PUT /businesses/{id}/status,
				   rather than a quiet dropdown on a settings form. */
				allowStatus={false}
				submitting={update.isPending}
				onSubmit={handleSubmit}
				onCancel={() => router.push(`/businesses/${id}`)}
			/>

			<Card className="mt-4 md:mt-6" title="Addresses and contacts">
				<p className="text-sm text-ink-muted">
					The salon&apos;s address and phone are set when it&apos;s created. Changing them
					afterwards isn&apos;t in the panel yet.
				</p>
			</Card>
		</div>
	);
}
