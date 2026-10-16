import React from "react";
import { cn } from "@/lib/utils/cn";

export function Skeleton({ className }: { className?: string }) {
	return (
		<span
			aria-hidden="true"
			className={cn("block animate-pulse rounded-lg bg-neutral-200 dark:bg-white/10", className)}
		/>
	);
}

/** A salon-card-shaped placeholder, so the Discover list doesn't jump when real
 *  rows replace it. */
export function SalonCardSkeleton() {
	return (
		<div className="flex gap-3 rounded-lg border border-border-default bg-surface-raised p-3 shadow-xs">
			<Skeleton className="size-20 shrink-0 rounded-md" />
			<div className="flex min-w-0 flex-1 flex-col gap-2 py-1">
				<Skeleton className="h-4 w-3/5" />
				<Skeleton className="h-3 w-2/5" />
				<Skeleton className="mt-auto h-3 w-1/3" />
			</div>
		</div>
	);
}
