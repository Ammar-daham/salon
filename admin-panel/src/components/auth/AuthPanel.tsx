import Link from "next/link";
import React from "react";
import { ChevronLeftIcon } from "@/icons";
import { cn } from "@/lib/utils/cn";

interface AuthPanelProps {
	title: string;
	description: React.ReactNode;
	back: { href: string; label: string };
	children: React.ReactNode;
	/**
	 * For a form taller than the screen: the panel scrolls, from the top. Centred, its top would be
	 * cut off, out of reach.
	 */
	long?: boolean;
}

/** The form half of the signed-out screens: a way back, a heading, and what goes under it. */
export default function AuthPanel({ title, description, back, children, long = false }: AuthPanelProps) {
	return (
		<div className={cn("flex flex-col flex-1 lg:w-1/2 w-full", long && "overflow-y-auto")}>
			<div className="w-full max-w-md sm:pt-10 mx-auto mb-5">
				<Link
					href={back.href}
					className="inline-flex items-center text-sm text-neutral-500 transition-colors hover:text-neutral-700 dark:text-neutral-400 dark:hover:text-neutral-300"
				>
					<ChevronLeftIcon />
					{back.label}
				</Link>
			</div>
			<div
				className={cn(
					"flex flex-col flex-1 w-full max-w-md mx-auto",
					long ? "justify-start pb-10" : "justify-center",
				)}
			>
				<div className="mb-5 sm:mb-8">
					<h1 className="mb-2 font-semibold text-neutral-800 text-h1 dark:text-white/90 sm:text-display">
						{title}
					</h1>
					<p className="text-sm text-neutral-500 dark:text-neutral-400">{description}</p>
				</div>
				{children}
			</div>
		</div>
	);
}
