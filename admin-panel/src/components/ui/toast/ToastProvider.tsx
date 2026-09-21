"use client";

import React, {
	createContext,
	useCallback,
	useContext,
	useEffect,
	useMemo,
	useRef,
	useState,
} from "react";
import { cn } from "@/lib/utils/cn";
import { CheckCircleIcon, CloseIcon, ErrorIcon, InfoIcon, AlertIcon } from "@/icons";

export type ToastTone = "success" | "error" | "warning" | "info";

export interface ToastOptions {
	title: string;
	description?: string;
	tone?: ToastTone;
	/** Milliseconds before auto-dismiss. Errors stay until dismissed. */
	duration?: number;
	action?: { label: string; onClick: () => void };
}

interface Toast extends Required<Pick<ToastOptions, "title" | "tone">> {
	id: number;
	description?: string;
	duration: number;
	action?: ToastOptions["action"];
}

interface ToastContextValue {
	toast: (options: ToastOptions) => void;
	dismiss: (id: number) => void;
}

const ToastContext = createContext<ToastContextValue | undefined>(undefined);

const toneStyles: Record<ToastTone, { wrap: string; icon: React.ReactNode }> = {
	success: {
		wrap: "border-success-200 bg-success-50 text-success-800 dark:border-success-700/40 dark:bg-success-500/10 dark:text-success-200",
		icon: <CheckCircleIcon className="size-5" />,
	},
	error: {
		wrap: "border-error-200 bg-error-50 text-error-800 dark:border-error-700/40 dark:bg-error-500/10 dark:text-error-200",
		icon: <ErrorIcon className="size-5" />,
	},
	warning: {
		wrap: "border-warning-200 bg-warning-50 text-warning-800 dark:border-warning-700/40 dark:bg-warning-500/10 dark:text-warning-200",
		icon: <AlertIcon className="size-5" />,
	},
	info: {
		wrap: "border-info-200 bg-info-50 text-info-800 dark:border-info-700/40 dark:bg-info-500/10 dark:text-info-200",
		icon: <InfoIcon className="size-5" />,
	},
};

export function ToastProvider({ children }: { children: React.ReactNode }) {
	const [toasts, setToasts] = useState<Toast[]>([]);
	const nextId = useRef(1);

	const dismiss = useCallback((id: number) => {
		setToasts((current) => current.filter((t) => t.id !== id));
	}, []);

	const toast = useCallback((options: ToastOptions) => {
		const tone = options.tone ?? "info";
		// Errors persist: an auto-dismissing failure message is one the user can
		// miss entirely.
		const duration = options.duration ?? (tone === "error" ? 0 : 5000);
		setToasts((current) => [
			...current,
			{
				id: nextId.current++,
				title: options.title,
				description: options.description,
				tone,
				duration,
				action: options.action,
			},
		]);
	}, []);

	const value = useMemo(() => ({ toast, dismiss }), [toast, dismiss]);

	return (
		<ToastContext.Provider value={value}>
			{children}
			<div
				className="pointer-events-none fixed bottom-4 right-4 z-80 flex w-full max-w-sm flex-col gap-3"
				role="region"
				aria-label="Notifications"
			>
				{toasts.map((t) => (
					<ToastItem key={t.id} toast={t} onDismiss={dismiss} />
				))}
			</div>
		</ToastContext.Provider>
	);
}

function ToastItem({ toast, onDismiss }: { toast: Toast; onDismiss: (id: number) => void }) {
	useEffect(() => {
		if (!toast.duration) return;
		const timer = setTimeout(() => onDismiss(toast.id), toast.duration);
		return () => clearTimeout(timer);
	}, [toast.id, toast.duration, onDismiss]);

	const tone = toneStyles[toast.tone];

	return (
		<div
			role={toast.tone === "error" ? "alert" : "status"}
			className={cn(
				"pointer-events-auto flex items-start gap-3 rounded-card border p-4 shadow-lg",
				tone.wrap,
			)}
		>
			<span className="mt-0.5 shrink-0">{tone.icon}</span>
			<div className="min-w-0 flex-1">
				<p className="text-sm font-medium">{toast.title}</p>
				{toast.description && <p className="mt-1 text-sm opacity-90">{toast.description}</p>}
				{toast.action && (
					<button
						type="button"
						onClick={() => {
							toast.action?.onClick();
							onDismiss(toast.id);
						}}
						className="mt-2 text-sm font-semibold underline underline-offset-2"
					>
						{toast.action.label}
					</button>
				)}
			</div>
			<button
				type="button"
				onClick={() => onDismiss(toast.id)}
				aria-label="Dismiss notification"
				className="shrink-0 rounded p-1 opacity-70 transition-opacity hover:opacity-100"
			>
				<CloseIcon className="size-4" />
			</button>
		</div>
	);
}

export function useToast() {
	const context = useContext(ToastContext);
	if (!context) throw new Error("useToast must be used within a ToastProvider");
	return context;
}
