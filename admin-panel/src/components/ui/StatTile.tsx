import React from "react";
import { cn } from "@/lib/utils/cn";

interface StatTileProps {
	label: string;
	value: React.ReactNode;
	icon?: React.ReactNode;
	hint?: string;
	loading?: boolean;
	className?: string;
}

export default function StatTile({
	label,
	value,
	icon,
	hint,
	loading = false,
	className,
}: StatTileProps) {
	return (
		<div
			className={cn(
				"rounded-card border border-border-default bg-surface-raised p-5 shadow-xs",
				className,
			)}
		>
			{icon && (
				<span className="mb-4 flex size-11 items-center justify-center rounded-lg bg-primary-50 text-primary-700 dark:bg-primary-500/15 dark:text-primary-300">
					{icon}
				</span>
			)}
			<p className="text-sm text-ink-muted">{label}</p>
			{loading ? (
				<span
					className="mt-2 block h-8 w-20 animate-pulse rounded bg-neutral-200 dark:bg-white/10"
					aria-hidden="true"
				/>
			) : (
				<p className="mt-1 text-display font-semibold leading-none tracking-tight text-ink">
					{value}
				</p>
			)}
			{hint && <p className="mt-2 text-xs text-ink-subtle">{hint}</p>}
		</div>
	);
}
