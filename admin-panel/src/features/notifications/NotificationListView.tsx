"use client";

import { useState } from "react";
import Link from "next/link";
import { getErrorMessage } from "@/lib/api/errors";
import { cn } from "@/lib/utils/cn";
import { notificationText } from "@/lib/resources/notifications/notifications.mappers";
import {
	useMarkAllNotificationsRead,
	useMarkNotificationRead,
	useNotificationPage,
	useUnreadNotificationCount,
} from "@/lib/resources/notifications/notifications.hooks";
import type { AppNotification } from "@/lib/resources/notifications/notifications.types";

import PageHeader from "@/components/ui/PageHeader";
import EmptyState from "@/components/ui/EmptyState";
import Pagination from "@/components/ui/Pagination";
import Button from "@/components/ui/button/Button";
import { SelectInput } from "@/components/ui/form/Field";
import { Skeleton } from "@/components/ui/Skeleton";
import { useToast } from "@/components/ui/toast/ToastProvider";
import { BellIcon, CheckLineIcon, ErrorIcon } from "@/icons";

const PAGE_SIZE = 20;

function formatWhen(iso: string): string {
	return new Date(iso).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
}

/** One notification, opening what it's about; opening an unread one marks it read. */
export function NotificationRow({
	notification,
	onOpen,
}: {
	notification: AppNotification;
	onOpen: (notification: AppNotification) => void;
}) {
	const text = notificationText(notification);
	const unread = notification.readAt == null;
	return (
		<li>
			<Link
				href={text.href}
				onClick={() => onOpen(notification)}
				className="flex items-start gap-3 px-5 py-4 transition-colors hover:bg-neutral-100 dark:hover:bg-white/5"
			>
				<span
					aria-hidden
					className={cn("mt-1.5 size-2 shrink-0 rounded-full", unread ? "bg-primary-500" : "bg-transparent")}
				/>
				<span className="min-w-0 flex-1">
					<span className={cn("block text-sm text-ink", unread && "font-semibold")}>
						{text.title}
						{unread && <span className="sr-only"> (unread)</span>}
					</span>
					{text.body && <span className="mt-0.5 block text-sm text-ink-muted">{text.body}</span>}
				</span>
				<time dateTime={notification.createdAt} className="shrink-0 text-xs text-ink-subtle">
					{formatWhen(notification.createdAt)}
				</time>
			</Link>
		</li>
	);
}

/** The signed-in user's notifications, newest first: today, a super admin's salon registrations. */
export default function NotificationListView() {
	const { toast } = useToast();
	const [unreadOnly, setUnreadOnly] = useState(false);
	const [page, setPage] = useState(1);

	const { data, isPending, isError, error, refetch } = useNotificationPage({
		page,
		size: PAGE_SIZE,
		unread: unreadOnly,
	});
	const unread = useUnreadNotificationCount();
	const markRead = useMarkNotificationRead();
	const markAll = useMarkAllNotificationsRead();
	const unreadCount = unread.data ?? 0;

	function handleOpen(notification: AppNotification) {
		if (notification.readAt == null) markRead.mutate(notification.id);
	}

	async function handleMarkAll() {
		try {
			await markAll.mutateAsync();
			toast({ tone: "success", title: "All caught up", description: "Every notification is marked read." });
		} catch (err) {
			toast({ tone: "error", title: "Couldn't mark them read", description: getErrorMessage(err) });
		}
	}

	return (
		<>
			<PageHeader
				title="Notifications"
				description={
					unreadCount > 0
						? `${unreadCount} unread. Opening one marks it read.`
						: "What's happened that you should know about."
				}
				actions={
					<Button
						variant="outline"
						startIcon={<CheckLineIcon className="size-4" />}
						onClick={handleMarkAll}
						loading={markAll.isPending}
						disabled={unreadCount === 0}
					>
						Mark all as read
					</Button>
				}
			/>

			<div className="mb-4 flex items-center gap-3">
				<SelectInput
					aria-label="Show"
					value={unreadOnly ? "unread" : "all"}
					onChange={(e) => {
						setUnreadOnly(e.target.value === "unread");
						setPage(1);
					}}
					className="sm:w-48"
				>
					<option value="all">All notifications</option>
					<option value="unread">Unread only</option>
				</SelectInput>
			</div>

			{isError ? (
				<EmptyState
					icon={<ErrorIcon className="size-6" />}
					title="Couldn't load your notifications"
					description={getErrorMessage(error)}
					action={<Button variant="outline" onClick={() => refetch()}>Try again</Button>}
				/>
			) : isPending ? (
				<div className="flex flex-col gap-3">
					{[0, 1, 2].map((i) => (
						<Skeleton key={i} className="h-16 rounded-card" />
					))}
				</div>
			) : data.items.length === 0 ? (
				<EmptyState
					icon={<BellIcon className="size-6" />}
					title={unreadOnly ? "No unread notifications" : "No notifications yet"}
					description={
						unreadOnly ? "You're all caught up." : "New salon registrations will show up here."
					}
				/>
			) : (
				<>
					<ul className="divide-y divide-border-default overflow-hidden rounded-card border border-border-default bg-surface-raised">
						{data.items.map((n) => (
							<NotificationRow key={n.id} notification={n} onOpen={handleOpen} />
						))}
					</ul>
					<Pagination
						page={data.page}
						pageCount={data.totalPages}
						total={data.totalItems}
						pageSize={data.size}
						onPageChange={setPage}
						className="mt-4"
					/>
				</>
			)}
		</>
	);
}
