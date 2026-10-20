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
				"flex flex-1 flex-col items-center justify-center px-6 py-16 text-center",
				className,
			)}
		>
			{icon && (
				<span className="mb-4 flex size-14 items-center justify-center rounded-full bg-primary-50 text-primary-600 dark:bg-primary-500/15 dark:text-primary-300">
					{icon}
				</span>
			)}
			<h3 className="text-title-3 font-semibold text-ink">{title}</h3>
			{description && (
				<p className="mt-1.5 max-w-xs text-subhead text-ink-muted">{description}</p>
			)}
			{action && <div className="mt-6">{action}</div>}
		</div>
	);
}
