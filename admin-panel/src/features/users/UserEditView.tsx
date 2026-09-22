"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useUser, useUpdateUser } from "@/lib/resources/users/users.hooks";
import type { UpdateUserInput } from "@/lib/resources/users/users.types";
import { getErrorMessage } from "@/lib/api/errors";
import PageHeader from "@/components/ui/PageHeader";
import EmptyState from "@/components/ui/EmptyState";
import Card from "@/components/ui/Card";
import Button from "@/components/ui/button/Button";
import { Skeleton } from "@/components/ui/Skeleton";
import { useToast } from "@/components/ui/toast/ToastProvider";
import { ChevronLeftIcon, ErrorIcon } from "@/icons";
import UserForm from "./UserForm";
import { initialsOf } from "./UserListView";

export default function UserEditView() {
	const params = useParams<{ id: string }>();
	const id = Number(params.id);
	const router = useRouter();
	const { toast } = useToast();

	const { data: user, isPending, isError, error, refetch } = useUser(Number.isNaN(id) ? null : id);
	const update = useUpdateUser();

	async function handleSubmit(input: UpdateUserInput) {
		try {
			await update.mutateAsync({ id, input });
			toast({ tone: "success", title: "Changes saved" });
		} catch (err) {
			toast({ tone: "error", title: "Couldn't save changes", description: getErrorMessage(err) });
		}
	}

	if (isError) {
		return (
			<EmptyState
				icon={<ErrorIcon className="size-6" />}
				title="Couldn't load this account"
				description={getErrorMessage(error)}
				action={<Button variant="outline" onClick={() => refetch()}>Try again</Button>}
			/>
		);
	}

	return (
		<>
			<Link
				href="/users"
				className="mb-4 inline-flex items-center gap-1 text-sm text-ink-muted transition-colors hover:text-ink"
			>
				<ChevronLeftIcon className="size-4" />
				All users
			</Link>

			{isPending || !user ? (
				<Skeleton className="h-96 rounded-card" />
			) : (
				<>
					<div className="mb-6 flex items-center gap-4">
						<span className="flex size-14 items-center justify-center rounded-full bg-primary-100 text-lg font-semibold text-primary-700 dark:bg-primary-500/20 dark:text-primary-200">
							{initialsOf(user)}
						</span>
						<PageHeader
							className="mb-0"
							title={`${user.firstName} ${user.lastName}`}
							description={user.email ?? undefined}
						/>
					</div>

					<div className="max-w-3xl">
						<UserForm
							mode="edit"
							initial={user}
							submitting={update.isPending}
							onSubmitUpdate={handleSubmit}
							onCancel={() => router.push("/users")}
						/>

						<Card className="mt-4 md:mt-6" title="Password and email">
							<p className="text-sm text-ink-muted">
								Neither can be changed after the account is created. The update endpoint
								only writes the name and role, and there is no password-reset endpoint —
								so a forgotten password currently means deleting the account and creating
								it again.
							</p>
						</Card>
					</div>
				</>
			)}
		</>
	);
}
