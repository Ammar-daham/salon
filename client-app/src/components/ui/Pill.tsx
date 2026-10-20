import React from "react";
import { cn } from "@/lib/utils/cn";

/**
 * A tappable filter chip (Discover's "Open now" / "Nearby" / category row).
 * Unlike `Badge`, this is a real `<button>` with a pressed/selected state.
 */
export default function Pill({
	selected = false,
	children,
	className,
	...rest
}: {
	selected?: boolean;
	children: React.ReactNode;
	className?: string;
} & Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, "className" | "children">) {
	return (
		<button
			type="button"
			aria-pressed={selected}
			className={cn(
				"inline-flex shrink-0 items-center gap-1.5 rounded-pill border px-3.5 py-2 text-footnote font-medium transition-colors active:scale-[0.97]",
				selected
					? "border-primary-solid bg-primary-solid text-white"
					: "border-border-default bg-surface-raised text-ink-muted hover:bg-neutral-50 dark:hover:bg-white/5",
				className,
			)}
			{...rest}
		>
			{children}
		</button>
	);
}
