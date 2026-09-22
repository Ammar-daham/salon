"use client";

import { useRouter } from "next/navigation";
import { useCreateUser } from "@/lib/resources/users/users.hooks";
import type { CreateUserInput } from "@/lib/resources/users/users.types";
import type { Role } from "@/lib/resources/auth/auth.types";
import { getErrorMessage, normalizeError } from "@/lib/api/errors";
import PageHeader from "@/components/ui/PageHeader";
import { useToast } from "@/components/ui/toast/ToastProvider";
import UserForm from "./UserForm";

interface UserCreateViewProps {
	title: string;
	description: string;
	/** Restricts the role select, e.g. the Add-employee flow. */
	allowedRoles?: Role[];
	/** Where to go after a successful create. */
	returnTo: string;
}

export default function UserCreateView({
	title,
	description,
	allowedRoles,
	returnTo,
}: UserCreateViewProps) {
	const router = useRouter();
	const { toast } = useToast();
	const create = useCreateUser();

	async function handleSubmit(input: CreateUserInput) {
		try {
			const created = await create.mutateAsync(input);
			toast({
				tone: "success",
				title: "Account created",
				description: `${created.firstName} ${created.lastName} can now sign in.`,
			});
			router.push(returnTo);
		} catch (err) {
			const apiError = normalizeError(err);
			toast({
				tone: "error",
				// 409 is the backend's duplicate-email/contact response; naming it
				// beats a generic failure message.
				title: apiError.kind === "conflict" ? "That email is already in use" : "Couldn't create account",
				description: getErrorMessage(err),
			});
		}
	}

	return (
		<>
			<PageHeader title={title} description={description} />
			<div className="max-w-3xl">
				<UserForm
					mode="create"
					allowedRoles={allowedRoles}
					submitting={create.isPending}
					onSubmitCreate={handleSubmit}
					onCancel={() => router.push(returnTo)}
				/>
			</div>
		</>
	);
}
