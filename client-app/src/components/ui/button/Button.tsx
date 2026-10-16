import React from "react";
import { cn } from "@/lib/utils/cn";

export type ButtonVariant = "primary" | "secondary" | "outline" | "ghost" | "destructive";
export type ButtonSize = "sm" | "md" | "lg";

interface ButtonProps extends Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, "type"> {
	children?: React.ReactNode;
	size?: ButtonSize;
	variant?: ButtonVariant;
	startIcon?: React.ReactNode;
	endIcon?: React.ReactNode;
	loading?: boolean;
	/** Explicit on purpose — a bare <button> inside a <form> defaults to "submit". */
	type?: "button" | "submit" | "reset";
	className?: string;
}

const sizeClasses: Record<ButtonSize, string> = {
	sm: "h-9 px-4 text-footnote gap-1.5",
	md: "h-11 px-5 text-callout gap-2",
	lg: "h-[52px] px-6 text-headline gap-2",
};

/**
 * `primary` is the one big rose CTA per screen (Book, Confirm, Continue).
 * `secondary` is a tinted rose fill for a lower-emphasis-but-still-brand action.
 * Large touch targets throughout (sm is still 36pt, same floor as Pro) since
 * this is a tap-first surface, not a mouse-first one.
 */
const variantClasses: Record<ButtonVariant, string> = {
	primary:
		"bg-primary-solid text-white shadow-sm hover:bg-primary-700 active:scale-[0.98] focus-visible:ring-primary-500/40 disabled:bg-primary-300 dark:disabled:bg-primary-800",
	secondary:
		"bg-primary-100 text-primary-700 hover:bg-primary-200 active:scale-[0.98] focus-visible:ring-primary-500/30 dark:bg-primary-500/15 dark:text-primary-200 dark:hover:bg-primary-500/25",
	destructive:
		"bg-error-500 text-white shadow-sm hover:bg-error-600 active:scale-[0.98] focus-visible:ring-error-500/40 disabled:bg-error-200",
	outline:
		"border border-border-strong bg-surface-raised text-ink shadow-xs hover:bg-neutral-50 active:scale-[0.98] focus-visible:ring-primary-500/30 dark:hover:bg-white/5",
	ghost:
		"text-ink-muted hover:bg-neutral-100 hover:text-ink active:scale-[0.98] focus-visible:ring-primary-500/30 dark:hover:bg-white/5",
};

const Button: React.FC<ButtonProps> = ({
	children,
	size = "md",
	variant = "primary",
	startIcon,
	endIcon,
	loading = false,
	type = "button",
	className,
	disabled,
	...rest
}) => {
	const isDisabled = disabled || loading;

	return (
		<button
			type={type}
			disabled={isDisabled}
			aria-busy={loading || undefined}
			className={cn(
				"inline-flex items-center justify-center rounded-xl font-semibold transition-all",
				"focus-visible:outline-none focus-visible:ring-4",
				sizeClasses[size],
				variantClasses[variant],
				isDisabled && "cursor-not-allowed opacity-60 active:scale-100",
				className,
			)}
			{...rest}
		>
			{loading ? (
				<span
					className="size-4 animate-spin rounded-full border-2 border-current/30 border-t-current"
					aria-hidden="true"
				/>
			) : (
				startIcon && <span className="flex items-center">{startIcon}</span>
			)}
			{children}
			{!loading && endIcon && <span className="flex items-center">{endIcon}</span>}
		</button>
	);
};

export default Button;
