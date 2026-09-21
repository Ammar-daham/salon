"use client";

import React from "react";
import Button from "@/components/ui/button/Button";
import { ChevronLeftIcon } from "@/icons";
import { cn } from "@/lib/utils/cn";

interface PaginationProps {
	page: number;
	pageCount: number;
	total: number;
	pageSize: number;
	onPageChange: (page: number) => void;
	className?: string;
}

export default function Pagination({
	page,
	pageCount,
	total,
	pageSize,
	onPageChange,
	className,
}: PaginationProps) {
	if (pageCount <= 1) return null;

	const first = (page - 1) * pageSize + 1;
	const last = Math.min(page * pageSize, total);

	return (
		<nav
			aria-label="Pagination"
			className={cn(
				"flex flex-col items-center justify-between gap-3 border-t border-border-default px-5 py-3.5 sm:flex-row",
				className,
			)}
		>
			<p className="text-sm text-ink-muted">
				Showing <span className="font-medium text-ink">{first}</span>–
				<span className="font-medium text-ink">{last}</span> of{" "}
				<span className="font-medium text-ink">{total}</span>
			</p>
			<div className="flex items-center gap-2">
				<Button
					size="sm"
					variant="outline"
					onClick={() => onPageChange(page - 1)}
					disabled={page <= 1}
					startIcon={<ChevronLeftIcon className="size-4" />}
				>
					Previous
				</Button>
				<span className="px-2 text-sm text-ink-muted" aria-current="page">
					{page} / {pageCount}
				</span>
				<Button
					size="sm"
					variant="outline"
					onClick={() => onPageChange(page + 1)}
					disabled={page >= pageCount}
					endIcon={<ChevronLeftIcon className="size-4 rotate-180" />}
				>
					Next
				</Button>
			</div>
		</nav>
	);
}
