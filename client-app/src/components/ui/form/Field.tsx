"use client";

import React, { useId } from "react";
import { cn } from "@/lib/utils/cn";

interface FieldProps {
	label: string;
	/** Rendered under the control when there is no error. */
	hint?: string;
	error?: string | null;
	required?: boolean;
	className?: string;
	children: (props: {
		id: string;
		"aria-invalid": boolean | undefined;
		"aria-describedby": string | undefined;
	}) => React.ReactNode;
}

/**
 * Wraps a control with its label, hint and error, and wires the accessibility
 * relationships so screen readers announce the error with the input rather than
 * leaving it as orphaned red text. Same render-prop shape as admin-panel's
 * Field, new visual sizing (bigger, rounder, tap-first).
 */
export default function Field({
	label,
	hint,
	error,
	required = false,
	className,
	children,
}: FieldProps) {
	const id = useId();
	const messageId = error ? `${id}-error` : hint ? `${id}-hint` : undefined;

	return (
		<div className={cn("flex flex-col gap-1.5", className)}>
			<label htmlFor={id} className="text-footnote font-medium text-ink-muted">
				{label}
				{required && (
					<span className="ml-0.5 text-error-500" aria-hidden="true">
						*
					</span>
				)}
			</label>

			{children({
				id,
				"aria-invalid": error ? true : undefined,
				"aria-describedby": messageId,
			})}

			{error ? (
				<p id={messageId} className="text-footnote text-error-500">
					{error}
				</p>
			) : hint ? (
				<p id={messageId} className="text-footnote text-ink-subtle">
					{hint}
				</p>
			) : null}
		</div>
	);
}

const controlBase =
	"w-full rounded-xl border bg-surface-raised px-4 text-callout text-ink shadow-xs transition-colors " +
	"placeholder:text-ink-subtle focus:outline-none focus:ring-4 " +
	"disabled:cursor-not-allowed disabled:opacity-60";

const controlTone = (invalid?: boolean) =>
	invalid
		? "border-error-400 focus:border-error-500 focus:ring-error-500/20"
		: "border-border-default focus:border-primary-500 focus:ring-primary-500/20";

export const TextInput = React.forwardRef<
	HTMLInputElement,
	React.InputHTMLAttributes<HTMLInputElement>
>(function TextInput({ className, ...props }, ref) {
	return (
		<input
			ref={ref}
			className={cn(controlBase, "h-12", controlTone(props["aria-invalid"] === true), className)}
			{...props}
		/>
	);
});
