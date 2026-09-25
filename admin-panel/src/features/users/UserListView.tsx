"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { can } from "@/lib/auth/permissions";
import { useDataTable } from "@/lib/table/useDataTable";
import { useDeleteUser, useUsers } from "@/lib/resources/users/users.hooks";
import { ROLE_LABELS, type Role } from "@/lib/resources/auth/auth.types";
import type { User } from "@/lib/resources/users/users.types";
import { getErrorMessage } from "@/lib/api/errors";

import PageHeader from "@/components/ui/PageHeader";
import DataTable, { type Column } from "@/components/ui/DataTable";
import SearchInput from "@/components/ui/SearchInput";
import Pagination from "@/components/ui/Pagination";
import EmptyState from "@/components/ui/EmptyState";
import StatusBadge, { type StatusTone } from "@/components/ui/StatusBadge";
import Button from "@/components/ui/button/Button";
import ConfirmDialog from "@/components/ui/modal/ConfirmDialog";
import { SelectInput } from "@/components/ui/form/Field";
import { useToast } from "@/components/ui/toast/ToastProvider";
import { ErrorIcon, GroupIcon, PlusIcon, TrashBinIcon } from "@/icons";

const ROLE_TONE: Record<Role, StatusTone> = {
	SUPER_ADMIN: "primary",
	ADMIN: "info",
	EMPLOYEE: "neutral",
	CUSTOMER: "neutral",
};

const ALL_ROLES: Role[] = ["SUPER_ADMIN", "ADMIN", "EMPLOYEE", "CUSTOMER"];

export function initialsOf(user: Pick<User, "firstName" | "lastName">) {
	return `${user.firstName?.[0] ?? ""}${user.lastName?.[0] ?? ""}`.toUpperCase() || "?";
}

export default function UserListView() {
	const { user: currentUser } = useAuth();
	const router = useRouter();
	const { toast } = useToast();

	const { data, isPending, isError, error, refetch } = useUsers();
	const remove = useDeleteUser();

	const [roleFilter, setRoleFilter] = useState<Role | "ALL">("ALL");
	const [pendingDelete, setPendingDelete] = useState<User | null>(null);

	const canCreate = can(currentUser, "user:create");
	const canDelete = can(currentUser, "user:delete");

	const filters = useMemo(
		() => [(u: User) => roleFilter === "ALL" || u.role === roleFilter],
		[roleFilter],
	);

	const table = useDataTable<User>({
		rows: data ?? [],
		searchAccessor: (u) => `${u.firstName} ${u.lastName} ${u.email ?? ""}`,
		sortAccessor: (u, key) => {
			switch (key) {
				case "name": return `${u.firstName} ${u.lastName}`;
				case "email": return u.email ?? "";
				case "role": return u.role;
				case "createdAt": return u.createdAt;
				default: return null;
			}
		},
		initialSort: { key: "name", direction: "asc" },
		filters,
		pageSize: 15,
	});

	async function handleDelete() {
		if (!pendingDelete) return;
		const target = pendingDelete;
		try {
			await remove.mutateAsync(target.id);
			toast({
				tone: "success",
				title: "Account deleted",
				description: `${target.firstName} ${target.lastName} was removed.`,
			});
			setPendingDelete(null);
		} catch (err) {
			toast({ tone: "error", title: "Couldn't delete account", description: getErrorMessage(err) });
		}
	}

	const columns: Column<User>[] = [
		{
			key: "name",
			header: "Name",
			sortable: true,
			render: (u) => (
				<div className="flex items-center gap-3">
					<span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-primary-100 text-xs font-semibold text-primary-700 dark:bg-primary-500/20 dark:text-primary-200">
						{initialsOf(u)}
					</span>
					<Link
						href={`/users/${u.id}`}
						className="font-medium text-ink hover:text-primary-700 dark:hover:text-primary-300"
					>
						{u.firstName} {u.lastName}
					</Link>
					{u.id === currentUser?.id && (
						<span className="rounded-full bg-neutral-100 px-2 py-0.5 text-xs text-ink-muted dark:bg-white/5">
							You
						</span>
					)}
				</div>
			),
		},
		{
			key: "email",
			header: "Email",
			sortable: true,
			render: (u) => <span className="text-ink-muted">{u.email ?? "—"}</span>,
		},
		{
			key: "role",
			header: "Role",
			sortable: true,
			render: (u) => <StatusBadge tone={ROLE_TONE[u.role]}>{ROLE_LABELS[u.role]}</StatusBadge>,
		},
		{
			key: "createdAt",
			header: "Joined",
			sortable: true,
			render: (u) => (
				<span className="text-ink-muted">
					{u.createdAt
						? new Date(u.createdAt).toLocaleDateString(undefined, {
								year: "numeric",
								month: "short",
								day: "numeric",
						  })
						: "—"}
				</span>
			),
		},
		{
			key: "actions",
			header: "",
			align: "right",
			render: (u) => (
				<div className="flex justify-end gap-1">
					<Button size="sm" variant="outline" onClick={() => router.push(`/users/${u.id}`)}>
						Edit
					</Button>
					{/* Never offer self-deletion — it would sign the admin out of an
					    account they can't recreate without another admin. */}
					{canDelete && u.id !== currentUser?.id && (
						<Button
							size="sm"
							variant="ghost"
							aria-label={`Delete ${u.firstName} ${u.lastName}`}
							onClick={() => setPendingDelete(u)}
							startIcon={<TrashBinIcon className="size-4" />}
							className="text-error-600 hover:bg-error-50 hover:text-error-700 dark:hover:bg-error-500/10"
						>
							Delete
						</Button>
					)}
				</div>
			),
		},
	];

	return (
		<>
			<PageHeader
				title="Users"
				description={
					currentUser?.role === "SUPER_ADMIN"
						? "Every account on the platform, across all salons."
						: "Every account in your salon."
				}
				actions={
					canCreate && (
						<Button
							startIcon={<PlusIcon className="size-4" />}
							onClick={() => router.push("/users/new")}
						>
							New user
						</Button>
					)
				}
			/>

			{isError ? (
				<EmptyState
					icon={<ErrorIcon className="size-6" />}
					title="Couldn't load users"
					description={getErrorMessage(error)}
					action={<Button variant="outline" onClick={() => refetch()}>Try again</Button>}
				/>
			) : (
				<>
					<div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
						<SearchInput
							value={table.search}
							onChange={table.setSearch}
							placeholder="Search by name or email…"
							aria-label="Search users"
							className="sm:max-w-sm"
						/>
						<SelectInput
							aria-label="Filter by role"
							value={roleFilter}
							onChange={(e) => setRoleFilter(e.target.value as Role | "ALL")}
							className="sm:w-48"
						>
							<option value="ALL">All roles</option>
							{ALL_ROLES.map((r) => (
								<option key={r} value={r}>
									{ROLE_LABELS[r]}
								</option>
							))}
						</SelectInput>
						{!isPending && (
							<p className="text-sm text-ink-subtle sm:ml-auto">
								{table.total} {table.total === 1 ? "account" : "accounts"}
							</p>
						)}
					</div>

					<DataTable
						columns={columns}
						rows={table.rows}
						rowKey={(u) => u.id}
						loading={isPending}
						sort={table.sort}
						onSort={table.toggleSort}
						empty={
							<EmptyState
								icon={<GroupIcon className="size-6" />}
								title={table.isFiltered ? "No accounts match" : "No accounts yet"}
								description={
									table.isFiltered
										? "Try a different search term, or clear the role filter."
										: "Create the first account to get started."
								}
								action={
									table.isFiltered ? (
										<Button
											variant="outline"
											onClick={() => {
												table.setSearch("");
												setRoleFilter("ALL");
											}}
										>
											Clear filters
										</Button>
									) : (
										canCreate && (
											<Button
												startIcon={<PlusIcon className="size-4" />}
												onClick={() => router.push("/users/new")}
											>
												New user
											</Button>
										)
									)
								}
							/>
						}
					/>

					{!isPending && table.total > 0 && (
						<div className="mt-px rounded-b-card border border-t-0 border-border-default bg-surface-raised">
							<Pagination
								page={table.page}
								pageCount={table.pageCount}
								total={table.total}
								pageSize={table.pageSize}
								onPageChange={table.setPage}
							/>
						</div>
					)}
				</>
			)}

			<ConfirmDialog
				isOpen={pendingDelete !== null}
				onClose={() => setPendingDelete(null)}
				onConfirm={handleDelete}
				loading={remove.isPending}
				title={`Delete ${pendingDelete?.firstName ?? ""} ${pendingDelete?.lastName ?? ""}?`.trim()}
				confirmLabel="Delete account"
				description={
					pendingDelete ? (
						<>
							This permanently removes the account for{" "}
							<strong className="text-ink">{pendingDelete.email ?? "this user"}</strong>, along
							with their addresses and contacts. They will no longer be able to sign in. This
							can&apos;t be undone.
						</>
					) : null
				}
			/>
		</>
	);
}
