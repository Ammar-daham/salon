"use client";

import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { can } from "@/lib/auth/permissions";
import { useCreateBusiness } from "@/lib/resources/businesses/businesses.hooks";
import type { BusinessInput } from "@/lib/resources/businesses/businesses.types";
import { getErrorMessage } from "@/lib/api/errors";
import PageHeader from "@/components/ui/PageHeader";
import { useToast } from "@/components/ui/toast/ToastProvider";
import BusinessForm from "./BusinessForm";

export default function BusinessCreateView() {
	const router = useRouter();
	const { user } = useAuth();
	const { toast } = useToast();
	const create = useCreateBusiness();

	// Status is writable exactly once, at creation: addBusiness defaults a null
	// status to PENDING, and the update path can't change it afterwards.
	const allowStatus = can(user, "business:create");

	async function handleSubmit(input: BusinessInput) {
		try {
			const created = await create.mutateAsync(input);
			toast({ tone: "success", title: "Salon created", description: `${created.name} is ready.` });
			router.push(`/businesses/${created.id}`);
		} catch (err) {
			toast({
				tone: "error",
				title: "Couldn't create salon",
				description: getErrorMessage(err),
			});
		}
	}

	return (
		<>
			<PageHeader
				title="New salon"
				description="Add a business to the platform."
			/>
			<div className="max-w-3xl">
				<BusinessForm
					mode="create"
					allowStatus={allowStatus}
					submitting={create.isPending}
					onSubmit={handleSubmit}
					onCancel={() => router.push("/businesses")}
				/>
			</div>
		</>
	);
}
