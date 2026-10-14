"use client";

import React, { useEffect, useRef } from "react";
import { X } from "lucide-react";
import { cn } from "@/lib/utils/cn";

interface BottomSheetProps {
	isOpen: boolean;
	onClose: () => void;
	children: React.ReactNode;
	className?: string;
	showCloseButton?: boolean;
	/** Labels the sheet for assistive tech. Pass the visible heading's id when there is one. */
	ariaLabel?: string;
}

/**
 * Nested/stacked sheets share one body-scroll lock, same reasoning as
 * admin-panel's Modal: releasing unconditionally on close would drop the lock
 * while another sheet is still open.
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

/**
 * The iOS-style sheet: slides up from the bottom, rounded top corners only, a
 * drag-handle bar standing in for an actual swipe-to-dismiss gesture (not
 * implemented here — tap the backdrop or the close button instead).
 */
export default function BottomSheet({
	isOpen,
	onClose,
	children,
	className,
	showCloseButton = true,
	ariaLabel,
}: BottomSheetProps) {
	const sheetRef = useRef<HTMLDivElement>(null);

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

	useEffect(() => {
		if (!isOpen) return;
		const previous = document.activeElement as HTMLElement | null;
		sheetRef.current?.focus();
		return () => previous?.focus?.();
	}, [isOpen]);

	if (!isOpen) return null;

	return (
		<div className="fixed inset-0 z-[var(--z-index-sheet)] flex items-end justify-center">
			<div
				className="fixed inset-0 size-full bg-black/40 backdrop-blur-sm"
				onClick={onClose}
				aria-hidden="true"
			/>
			<div
				ref={sheetRef}
				role="dialog"
				aria-modal="true"
				aria-label={ariaLabel}
				tabIndex={-1}
				className={cn(
					"relative max-h-[90dvh] w-full max-w-[480px] overflow-y-auto rounded-t-xl border-t border-border-default bg-surface-raised pb-[env(safe-area-inset-bottom)] shadow-xl focus:outline-none",
					className,
				)}
				onClick={(e) => e.stopPropagation()}
			>
				<div className="sticky top-0 z-10 flex justify-center bg-surface-raised pt-2.5">
					<span className="h-1.5 w-10 rounded-pill bg-border-strong" aria-hidden="true" />
				</div>
				{showCloseButton && (
					<button
						type="button"
						onClick={onClose}
						aria-label="Close"
						className="absolute right-4 top-4 z-10 flex size-8 items-center justify-center rounded-full bg-neutral-100 text-ink-subtle transition-colors hover:text-ink dark:bg-white/10"
					>
						<X className="size-4" />
					</button>
				)}
				{children}
			</div>
		</div>
	);
}
