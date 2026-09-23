import React from "react";
import { cn } from "@/lib/utils/cn";

interface EmptyStateProps {
	icon?: React.ReactNode;
	title: string;
	description?: string;
	action?: React.ReactNode;
	className?: string;
}

export default function EmptyState({
	icon,
	title,
	description,
	action,
	className,
}: EmptyStateProps) {
	return (
		<div
			className={cn(
				"flex flex-col items-center justify-center rounded-card border border-dashed border-border-strong bg-surface-raised px-6 py-14 text-center",
				className,
			)}
		>
			{icon && (
				<span className="mb-4 flex size-12 items-center justify-center rounded-full bg-neutral-100 text-neutral-500 dark:bg-white/5 dark:text-neutral-400">
					{icon}
				</span>
			)}
			<h3 className="text-base font-semibold text-ink">{title}</h3>
			{description && (
				<p className="mt-1.5 max-w-sm text-sm text-ink-muted">{description}</p>
			)}
			{action && <div className="mt-6">{action}</div>}
		</div>
	);
}
