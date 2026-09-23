"use client";

import React from "react";
import { Modal } from "./index";
import Button from "@/components/ui/button/Button";
import { TrashBinIcon, AlertIcon } from "@/icons";

interface ConfirmDialogProps {
	isOpen: boolean;
	onClose: () => void;
	onConfirm: () => void | Promise<void>;
	title: string;
	/**
	 * Say what will actually happen, naming the cascade where there is one —
	 * "…and its 12 services". Vague confirmations train people to click through.
	 */
	description: React.ReactNode;
	confirmLabel?: string;
	cancelLabel?: string;
	destructive?: boolean;
	loading?: boolean;
}

export default function ConfirmDialog({
	isOpen,
	onClose,
	onConfirm,
	title,
	description,
	confirmLabel = "Confirm",
	cancelLabel = "Cancel",
	destructive = true,
	loading = false,
}: ConfirmDialogProps) {
	return (
		<Modal
			isOpen={isOpen}
			onClose={onClose}
			ariaLabel={title}
			showCloseButton={false}
			className="max-w-md"
		>
			<div className="p-6">
				<div className="flex gap-4">
					<span
						className={
							destructive
								? "flex size-11 shrink-0 items-center justify-center rounded-full bg-error-50 text-error-600 dark:bg-error-500/15 dark:text-error-300"
								: "flex size-11 shrink-0 items-center justify-center rounded-full bg-warning-50 text-warning-700 dark:bg-warning-500/15 dark:text-warning-300"
						}
					>
						{destructive ? (
							<TrashBinIcon className="size-5" />
						) : (
							<AlertIcon className="size-5" />
						)}
					</span>
					<div className="min-w-0">
						<h2 className="text-base font-semibold text-ink">{title}</h2>
						<div className="mt-1.5 text-sm text-ink-muted">{description}</div>
					</div>
				</div>

				<div className="mt-6 flex justify-end gap-3">
					<Button variant="outline" onClick={onClose} disabled={loading}>
						{cancelLabel}
					</Button>
					{/* Icon + verb, never colour alone. */}
					<Button
						variant={destructive ? "destructive" : "primary"}
						onClick={onConfirm}
						loading={loading}
						startIcon={destructive ? <TrashBinIcon className="size-4" /> : undefined}
					>
						{confirmLabel}
					</Button>
				</div>
			</div>
		</Modal>
	);
}
