import React from "react";
import { cn } from "@/lib/utils/cn";

export function Skeleton({ className }: { className?: string }) {
	return (
		<span
			aria-hidden="true"
			className={cn("block animate-pulse rounded bg-neutral-200 dark:bg-white/10", className)}
		/>
	);
}

/** Placeholder rows matching a table's column count, so loading doesn't collapse
 *  the layout and shift everything when data lands. */
export function TableSkeleton({ rows = 5, columns = 4 }: { rows?: number; columns?: number }) {
	return (
		<div className="divide-y divide-border-default" role="status" aria-label="Loading">
			{Array.from({ length: rows }).map((_, r) => (
				<div key={r} className="flex items-center gap-4 px-5 py-4">
					{Array.from({ length: columns }).map((__, c) => (
						<Skeleton key={c} className={cn("h-4 flex-1", c === 0 && "max-w-[180px]")} />
					))}
				</div>
			))}
		</div>
	);
}
