"use client";

import React from "react";
import { cn } from "@/lib/utils/cn";
import { CloseIcon } from "@/icons";

interface SearchInputProps {
	value: string;
	onChange: (value: string) => void;
	placeholder?: string;
	className?: string;
	"aria-label"?: string;
}

export default function SearchInput({
	value,
	onChange,
	placeholder = "Search…",
	className,
	"aria-label": ariaLabel = "Search",
}: SearchInputProps) {
	return (
		<div className={cn("relative", className)}>
			<span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-ink-subtle">
				<svg className="size-5" viewBox="0 0 20 20" fill="none" aria-hidden="true">
					<path
						fillRule="evenodd"
						clipRule="evenodd"
						d="M3.04175 9.37363C3.04175 5.87693 5.87711 3.04199 9.37508 3.04199C12.8731 3.04199 15.7084 5.87693 15.7084 9.37363C15.7084 12.8703 12.8731 15.7053 9.37508 15.7053C5.87711 15.7053 3.04175 12.8703 3.04175 9.37363ZM9.37508 1.54199C5.04902 1.54199 1.54175 5.04817 1.54175 9.37363C1.54175 13.6991 5.04902 17.2053 9.37508 17.2053C11.2674 17.2053 13.003 16.5344 14.357 15.4176L17.177 18.238C17.4699 18.5309 17.9448 18.5309 18.2377 18.238C18.5306 17.9451 18.5306 17.4703 18.2377 17.1774L15.418 14.3573C16.5365 13.0033 17.2084 11.2669 17.2084 9.37363C17.2084 5.04817 13.7011 1.54199 9.37508 1.54199Z"
						fill="currentColor"
					/>
				</svg>
			</span>
			<input
				type="search"
				value={value}
				aria-label={ariaLabel}
				onChange={(e) => onChange(e.target.value)}
				placeholder={placeholder}
				className="h-11 w-full rounded-lg border border-border-default bg-surface-raised pl-11 pr-10 text-sm text-ink shadow-xs transition-colors placeholder:text-ink-subtle focus:border-primary-400 focus:outline-none focus:ring-4 focus:ring-primary-500/20"
			/>
			{value && (
				<button
					type="button"
					onClick={() => onChange("")}
					aria-label="Clear search"
					className="absolute right-3 top-1/2 -translate-y-1/2 rounded p-1 text-ink-subtle transition-colors hover:text-ink"
				>
					<CloseIcon className="size-4" />
				</button>
			)}
		</div>
	);
}
