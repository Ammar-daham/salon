import React from "react";
import { cn } from "@/lib/utils/cn";

interface CardProps {
	title?: string;
	description?: string;
	actions?: React.ReactNode;
	children?: React.ReactNode;
	className?: string;
	bodyClassName?: string;
}

export default function Card({
	title,
	description,
	actions,
	children,
	className,
	bodyClassName,
}: CardProps) {
	return (
		<section
			className={cn(
				"rounded-card border border-border-default bg-surface-raised shadow-xs",
				className,
			)}
		>
			{(title || actions) && (
				<header className="flex items-start justify-between gap-4 border-b border-border-default px-5 py-4">
					<div className="min-w-0">
						{title && <h2 className="text-base font-semibold text-ink">{title}</h2>}
						{description && <p className="mt-1 text-sm text-ink-muted">{description}</p>}
					</div>
					{actions && <div className="flex shrink-0 items-center gap-2">{actions}</div>}
				</header>
			)}
			<div className={cn("p-5", bodyClassName)}>{children}</div>
		</section>
	);
}
