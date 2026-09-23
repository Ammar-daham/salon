"use client";
import React, { useEffect, useRef } from "react";
import { cn } from "@/lib/utils/cn";
import { CloseIcon } from "@/icons";

interface ModalProps {
	isOpen: boolean;
	onClose: () => void;
	className?: string;
	children: React.ReactNode;
	showCloseButton?: boolean;
	isFullscreen?: boolean;
	/** Labels the dialog for assistive tech. Pass the visible heading's id when
	 *  there is one. */
	ariaLabel?: string;
}

/**
 * Nested/stacked modals share one body-scroll lock. The old implementation set
 * `overflow = "unset"` whenever any modal closed, which released the lock while
 * another was still open.
 */
let lockCount = 0;

function lockBodyScroll() {
	lockCount += 1;
	document.body.style.overflow = "hidden";
}

function releaseBodyScroll() {
	lockCount = Math.max(0, lockCount - 1);
	if (lockCount === 0) document.body.style.overflow = "";
}

export const Modal: React.FC<ModalProps> = ({
	isOpen,
	onClose,
	children,
	className,
	showCloseButton = true,
	isFullscreen = false,
	ariaLabel,
}) => {
	const modalRef = useRef<HTMLDivElement>(null);

	useEffect(() => {
		if (!isOpen) return;
		const handleEscape = (event: KeyboardEvent) => {
			if (event.key === "Escape") onClose();
		};
		document.addEventListener("keydown", handleEscape);
		return () => document.removeEventListener("keydown", handleEscape);
	}, [isOpen, onClose]);

	useEffect(() => {
		if (!isOpen) return;
		lockBodyScroll();
		return releaseBodyScroll;
	}, [isOpen]);

	// Move focus into the dialog so keyboard users don't stay behind it.
	useEffect(() => {
		if (!isOpen) return;
		const previous = document.activeElement as HTMLElement | null;
		modalRef.current?.focus();
		return () => previous?.focus?.();
	}, [isOpen]);

	if (!isOpen) return null;

	return (
		<div className="fixed inset-0 z-60 flex items-center justify-center overflow-y-auto p-4">
			{!isFullscreen && (
				<div
					className="fixed inset-0 size-full bg-neutral-900/40 backdrop-blur-sm"
					onClick={onClose}
					aria-hidden="true"
				/>
			)}
			<div
				ref={modalRef}
				role="dialog"
				aria-modal="true"
				aria-label={ariaLabel}
				tabIndex={-1}
				className={cn(
					isFullscreen
						? "size-full"
						: "relative w-full rounded-card border border-border-default bg-surface-raised shadow-xl focus:outline-none",
					className,
				)}
				onClick={(e) => e.stopPropagation()}
			>
				{showCloseButton && (
					<button
						type="button"
						onClick={onClose}
						aria-label="Close dialog"
						className="absolute right-4 top-4 z-10 flex size-9 items-center justify-center rounded-full text-ink-subtle transition-colors hover:bg-neutral-100 hover:text-ink dark:hover:bg-white/5"
					>
						<CloseIcon className="size-5" />
					</button>
				)}
				{children}
			</div>
		</div>
	);
};
