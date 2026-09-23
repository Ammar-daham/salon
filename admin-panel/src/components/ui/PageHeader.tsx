import React from "react";
import { cn } from "@/lib/utils/cn";

interface PageHeaderProps {
	title: string;
	description?: string;
	/**
	 * Set when any repository feeding this page is mock-backed. Renders the
	 * "Sample data" pill — the review artifact that makes the real/mock boundary
	 * auditable at a glance, so nobody is taught a false model of the product.
	 */
	sampleData?: boolean;
	actions?: React.ReactNode;
	className?: string;
}

export default function PageHeader({
	title,
	description,
	sampleData = false,
	actions,
	className,
}: PageHeaderProps) {
	return (
		<div
			className={cn(
				"mb-6 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between",
				className,
			)}
		>
			<div className="min-w-0">
				<div className="flex flex-wrap items-center gap-3">
					<h1 className="text-h1 font-semibold tracking-tight text-ink">{title}</h1>
					{sampleData && (
						<span className="inline-flex items-center gap-1.5 rounded-full border border-warning-200 bg-warning-50 px-2.5 py-1 text-xs font-medium text-warning-700 dark:border-warning-700/40 dark:bg-warning-500/10 dark:text-warning-300">
							<span className="size-1.5 rounded-full bg-warning-500" aria-hidden="true" />
							Sample data
						</span>
					)}
				</div>
				{description && (
					<p className="mt-1.5 max-w-2xl text-sm text-ink-muted">{description}</p>
				)}
			</div>
			{actions && <div className="flex shrink-0 items-center gap-3">{actions}</div>}
		</div>
	);
}
