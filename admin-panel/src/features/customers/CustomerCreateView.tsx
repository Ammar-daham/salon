"use client";

import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { resolveBusinessScope } from "@/lib/auth/scope";
import { useCreateCustomer } from "@/lib/resources/customers/customers.hooks";
import type { CustomerInput } from "@/lib/resources/customers/customers.types";
import { getErrorMessage } from "@/lib/api/errors";
import PageHeader from "@/components/ui/PageHeader";
import EmptyState from "@/components/ui/EmptyState";
import { useToast } from "@/components/ui/toast/ToastProvider";
import { UserIcon } from "@/icons";
import CustomerForm from "./CustomerForm";

export default function CustomerCreateView() {
	const router = useRouter();
	const { user } = useAuth();
	const { toast } = useToast();
	const create = useCreateCustomer();
	const scope = resolveBusinessScope(user);

	if (scope.kind === "unresolved") {
		return (
			<>
				<PageHeader title="Add client" />
				<EmptyState
					icon={<UserIcon className="size-6" />}
					title="Your account isn't linked to a salon yet"
					description="Ask a platform administrator to attach your account to a business before adding clients."
				/>
			</>
		);
	}

	async function handleSubmit(input: CustomerInput, pickedBusinessId: number | null) {
		// A SUPER_ADMIN picked a salon in the form; anyone else adds to their own.
		const businessId = scope.kind === "business" ? scope.businessId : pickedBusinessId;
		if (businessId == null) return;

		try {
			const customer = await create.mutateAsync({ businessId, input });
			toast({
				tone: "success",
				title: "Client added",
				description: `${customer.firstName} ${customer.lastName} is now on ${customer.businessName}'s client list.`,
			});
			router.push(`/customers/${customer.id}`);
		} catch (err) {
			toast({ tone: "error", title: "Couldn't add client", description: getErrorMessage(err) });
		}
	}

	return (
		<>
			<PageHeader title="Add client" description="Add a walk-in or phone booking to your client list." />
			<div className="max-w-3xl">
				<CustomerForm
					pickBusiness={scope.kind === "platform"}
					submitLabel="Add client"
					submitting={create.isPending}
					onSubmit={handleSubmit}
					onCancel={() => router.push("/customers")}
				/>
			</div>
		</>
	);
}
