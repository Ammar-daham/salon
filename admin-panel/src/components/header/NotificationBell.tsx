"use client";

import { useState } from "react";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { can } from "@/lib/auth/permissions";
import { getErrorMessage } from "@/lib/api/errors";
import { unreadBadge } from "@/lib/resources/notifications/notifications.mappers";
import {
	useMarkAllNotificationsRead,
	useMarkNotificationRead,
	useNotificationPage,
	useUnreadNotificationCount,
} from "@/lib/resources/notifications/notifications.hooks";
import type { AppNotification } from "@/lib/resources/notifications/notifications.types";
import { NotificationRow } from "@/features/notifications/NotificationListView";
import { Dropdown } from "@/components/ui/dropdown/Dropdown";
import { Skeleton } from "@/components/ui/Skeleton";
import { useToast } from "@/components/ui/toast/ToastProvider";
import { BellIcon } from "@/icons";

const LATEST = 5;

/**
 * The header's bell: how many notifications are unread, and the latest few. The count is asked for
 * again every minute; the latest are only loaded while the dropdown is open.
 */
export default function NotificationBell() {
	const { user } = useAuth();
	const { toast } = useToast();
	const allowed = can(user, "notification:view");
	const [isOpen, setIsOpen] = useState(false);

	const unread = useUnreadNotificationCount(allowed);
	const latest = useNotificationPage({ page: 1, size: LATEST }, allowed && isOpen);
	const markRead = useMarkNotificationRead();
	const markAll = useMarkAllNotificationsRead();

	if (!allowed) return null;

	const count = unread.data ?? 0;
	const badge = unreadBadge(count);

	function close() {
		setIsOpen(false);
	}

	function handleOpen(notification: AppNotification) {
		if (notification.readAt == null) markRead.mutate(notification.id);
		close();
	}

	async function handleMarkAll() {
		try {
			await markAll.mutateAsync();
		} catch (err) {
			toast({ tone: "error", title: "Couldn't mark them read", description: getErrorMessage(err) });
		}
	}

	return (
		<div className="relative">
			<button
				type="button"
				onClick={() => setIsOpen((open) => !open)}
				aria-expanded={isOpen}
				aria-haspopup="menu"
				aria-label={count > 0 ? `Notifications, ${count} unread` : "Notifications"}
				className="dropdown-toggle relative flex h-11 w-11 items-center justify-center rounded-full border border-neutral-200 bg-white text-neutral-500 transition-colors hover:bg-neutral-100 hover:text-neutral-700 dark:border-neutral-800 dark:bg-neutral-900 dark:text-neutral-400 dark:hover:bg-neutral-800 dark:hover:text-white"
			>
				<BellIcon className="size-5" />
				{badge && (
					<span
						aria-hidden
						className="absolute -right-0.5 -top-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-primary-solid px-1 text-[11px] font-semibold leading-none text-white"
					>
						{badge}
					</span>
				)}
			</button>

			{/* On a phone the bell sits at the left of the bar, so the panel spans the screen instead. */}
			<Dropdown
				isOpen={isOpen}
				onClose={close}
				className="w-[340px] p-0 max-lg:fixed max-lg:inset-x-4 max-lg:top-20 max-lg:w-auto"
			>
				<div className="flex items-center justify-between gap-3 border-b border-border-default px-5 py-3">
					<span className="text-sm font-semibold text-ink">Notifications</span>
					<button
						type="button"
						onClick={handleMarkAll}
						disabled={count === 0 || markAll.isPending}
						className="text-xs font-medium text-primary-700 hover:underline disabled:cursor-default disabled:text-ink-subtle disabled:no-underline dark:text-primary-300"
					>
						Mark all as read
					</button>
				</div>

				{latest.isPending ? (
					<div className="flex flex-col gap-2 p-4">
						<Skeleton className="h-10" />
						<Skeleton className="h-10" />
					</div>
				) : latest.isError ? (
					<p className="px-5 py-6 text-sm text-ink-muted">{getErrorMessage(latest.error)}</p>
				) : latest.data.items.length === 0 ? (
					<p className="px-5 py-6 text-sm text-ink-muted">You&apos;re all caught up.</p>
				) : (
					<ul className="max-h-96 divide-y divide-border-default overflow-y-auto">
						{latest.data.items.map((n) => (
							<NotificationRow key={n.id} notification={n} onOpen={handleOpen} />
						))}
					</ul>
				)}

				<div className="border-t border-border-default px-5 py-3 text-center">
					<Link
						href="/notifications"
						onClick={close}
						className="text-sm font-medium text-primary-700 hover:underline dark:text-primary-300"
					>
						View all
					</Link>
				</div>
			</Dropdown>
		</div>
	);
}
