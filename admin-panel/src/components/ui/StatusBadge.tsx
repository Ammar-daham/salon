import React from "react";
import { cn } from "@/lib/utils/cn";

export type StatusTone = "success" | "warning" | "error" | "info" | "neutral" | "primary";

const toneClasses: Record<StatusTone, string> = {
	success:
		"bg-success-50 text-success-700 ring-success-200 dark:bg-success-500/15 dark:text-success-300 dark:ring-success-700/40",
	warning:
		"bg-warning-50 text-warning-700 ring-warning-200 dark:bg-warning-500/15 dark:text-warning-300 dark:ring-warning-700/40",
	error:
		"bg-error-50 text-error-700 ring-error-200 dark:bg-error-500/15 dark:text-error-300 dark:ring-error-700/40",
	info: "bg-info-50 text-info-700 ring-info-200 dark:bg-info-500/15 dark:text-info-300 dark:ring-info-700/40",
	primary:
		"bg-primary-50 text-primary-700 ring-primary-200 dark:bg-primary-500/15 dark:text-primary-300 dark:ring-primary-700/40",
	neutral:
		"bg-neutral-100 text-ink-muted ring-neutral-200 dark:bg-white/5 dark:text-neutral-300 dark:ring-white/10",
};

const dotClasses: Record<StatusTone, string> = {
	success: "bg-success-500",
	warning: "bg-warning-500",
	error: "bg-error-500",
	info: "bg-info-500",
	primary: "bg-primary-500",
	neutral: "bg-neutral-400",
};

/**
 * Status always ships with its label — never colour alone. The dot is a second,
 * redundant channel, not the message.
 */
export default function StatusBadge({
	tone = "neutral",
	children,
	className,
}: {
	tone?: StatusTone;
	children: React.ReactNode;
	className?: string;
}) {
	return (
		<span
			className={cn(
				"inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ring-1 ring-inset",
				toneClasses[tone],
				className,
			)}
		>
			<span className={cn("size-1.5 rounded-full", dotClasses[tone])} aria-hidden="true" />
			{children}
		</span>
	);
}
