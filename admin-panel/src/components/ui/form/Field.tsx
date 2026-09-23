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
 * leaving it as orphaned red text.
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
			<label htmlFor={id} className="text-sm font-medium text-ink">
				{label}
				{required && (
					<span className="ml-0.5 text-error-600" aria-hidden="true">
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
				<p id={messageId} className="text-sm text-error-600 dark:text-error-300">
					{error}
				</p>
			) : hint ? (
				<p id={messageId} className="text-sm text-ink-subtle">
					{hint}
				</p>
			) : null}
		</div>
	);
}

const controlBase =
	"w-full rounded-lg border bg-surface-raised px-4 text-sm text-ink shadow-xs transition-colors " +
	"placeholder:text-ink-subtle focus:outline-none focus:ring-4 " +
	"disabled:cursor-not-allowed disabled:opacity-60";

const controlTone = (invalid?: boolean) =>
	invalid
		? "border-error-400 focus:border-error-500 focus:ring-error-500/20"
		: "border-border-default focus:border-primary-400 focus:ring-primary-500/20";

export const TextInput = React.forwardRef<
	HTMLInputElement,
	React.InputHTMLAttributes<HTMLInputElement>
>(function TextInput({ className, ...props }, ref) {
	return (
		<input
			ref={ref}
			className={cn(controlBase, "h-11", controlTone(props["aria-invalid"] === true), className)}
			{...props}
		/>
	);
});

export const TextareaInput = React.forwardRef<
	HTMLTextAreaElement,
	React.TextareaHTMLAttributes<HTMLTextAreaElement>
>(function TextareaInput({ className, rows = 4, ...props }, ref) {
	return (
		<textarea
			ref={ref}
			rows={rows}
			className={cn(
				controlBase,
				"py-2.5",
				controlTone(props["aria-invalid"] === true),
				className,
			)}
			{...props}
		/>
	);
});

export const SelectInput = React.forwardRef<
	HTMLSelectElement,
	React.SelectHTMLAttributes<HTMLSelectElement>
>(function SelectInput({ className, children, ...props }, ref) {
	return (
		<select
			ref={ref}
			className={cn(
				controlBase,
				"h-11 appearance-none pr-10",
				controlTone(props["aria-invalid"] === true),
				// Chevron drawn inline so the control needs no wrapper element.
				"bg-[length:20px] bg-[right_0.75rem_center] bg-no-repeat",
				"bg-[url('data:image/svg+xml;utf8,<svg xmlns=%22http://www.w3.org/2000/svg%22 fill=%22none%22 viewBox=%220 0 24 24%22 stroke=%22%237a716a%22 stroke-width=%221.5%22><path stroke-linecap=%22round%22 stroke-linejoin=%22round%22 d=%22M19 9l-7 7-7-7%22/></svg>')]",
				className,
			)}
			{...props}
		>
			{children}
		</select>
	);
});
