import React from "react";
import { cn } from "@/lib/utils/cn";

export type ButtonVariant = "primary" | "outline" | "ghost" | "subtle" | "destructive";
export type ButtonSize = "sm" | "md" | "lg";

interface ButtonProps extends Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, "type"> {
	children?: React.ReactNode;
	size?: ButtonSize;
	variant?: ButtonVariant;
	startIcon?: React.ReactNode;
	endIcon?: React.ReactNode;
	loading?: boolean;
	/**
	 * Explicit and required-by-default in practice: the old Button had no `type`
	 * prop at all, so every instance inside a <form> silently defaulted to
	 * "submit".
	 */
	type?: "button" | "submit" | "reset";
	className?: string;
}

const sizeClasses: Record<ButtonSize, string> = {
	sm: "h-9 px-3.5 text-sm gap-1.5",
	md: "h-11 px-5 text-sm gap-2",
	lg: "h-12 px-6 text-base gap-2",
};

/**
 * The solid fills are the 600 step, not 500. White on primary-500 measures 3.80
 * which fails AA for body-sized text; primary-500 is reserved for identity and
 * non-text UI where the 3:1 threshold applies.
 *
 * `destructive` uses error-600 rather than error-500 to hold distance from rose
 * — and it never travels alone: destructive controls also carry an icon and a
 * verb, and route through ConfirmDialog.
 */
const variantClasses: Record<ButtonVariant, string> = {
	primary:
		"bg-primary-solid text-white shadow-xs hover:bg-primary-700 focus-visible:ring-primary-500/40 disabled:bg-primary-300 dark:disabled:bg-primary-800",
	destructive:
		"bg-error-600 text-white shadow-xs hover:bg-error-700 focus-visible:ring-error-500/40 disabled:bg-error-300",
	outline:
		"border border-border-strong bg-surface-raised text-ink shadow-xs hover:bg-neutral-50 focus-visible:ring-primary-500/30 dark:hover:bg-white/5",
	subtle:
		"bg-neutral-100 text-ink hover:bg-neutral-200 focus-visible:ring-primary-500/30 dark:bg-white/5 dark:hover:bg-white/10",
	ghost:
		"text-ink-muted hover:bg-neutral-100 hover:text-ink focus-visible:ring-primary-500/30 dark:hover:bg-white/5",
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
				"inline-flex items-center justify-center rounded-lg font-medium transition-colors",
				"focus-visible:outline-none focus-visible:ring-4",
				sizeClasses[size],
				variantClasses[variant],
				isDisabled && "cursor-not-allowed opacity-60",
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
