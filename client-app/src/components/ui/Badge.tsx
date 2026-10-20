import React from "react";
import { cn } from "@/lib/utils/cn";

export type BadgeTone = "neutral" | "primary" | "success" | "warning" | "error";

const toneClasses: Record<BadgeTone, string> = {
	neutral: "bg-neutral-100 text-ink-muted dark:bg-white/10",
	primary: "bg-primary-100 text-primary-700 dark:bg-primary-500/20 dark:text-primary-200",
	success: "bg-success-100 text-success-700 dark:bg-success-500/20 dark:text-success-200",
	warning: "bg-warning-100 text-warning-700 dark:bg-warning-500/20 dark:text-warning-200",
	error: "bg-error-100 text-error-700 dark:bg-error-500/20 dark:text-error-200",
};

export default function Badge({
	tone = "neutral",
	children,
	className,
}: {
	tone?: BadgeTone;
	children: React.ReactNode;
	className?: string;
}) {
	return (
		<span
			className={cn(
				"inline-flex items-center rounded-pill px-2.5 py-1 text-caption-1 font-semibold",
				toneClasses[tone],
				className,
			)}
		>
			{children}
		</span>
	);
}
