import React from "react";
import { cn } from "@/lib/utils/cn";

interface CardProps {
	title?: string;
	description?: string;
	actions?: React.ReactNode;
	children?: React.ReactNode;
	className?: string;
	bodyClassName?: string;
	/** onClick turns the whole card into a tap target (e.g. a salon list row). */
	onClick?: () => void;
}

export default function Card({
	title,
	description,
	actions,
	children,
	className,
	bodyClassName,
	onClick,
}: CardProps) {
	const Tag = onClick ? "button" : "section";

	return (
		<Tag
			type={onClick ? "button" : undefined}
			onClick={onClick}
			className={cn(
				"block w-full rounded-lg border border-border-default bg-surface-raised text-left shadow-xs",
				onClick && "transition-transform active:scale-[0.98]",
				className,
			)}
		>
			{(title || actions) && (
				<header className="flex items-start justify-between gap-4 px-4 pt-4">
					<div className="min-w-0">
						{title && <h2 className="text-headline font-semibold text-ink">{title}</h2>}
						{description && <p className="mt-0.5 text-subhead text-ink-muted">{description}</p>}
					</div>
					{actions && <div className="flex shrink-0 items-center gap-2">{actions}</div>}
				</header>
			)}
			<div className={cn("p-4", bodyClassName)}>{children}</div>
		</Tag>
	);
}
