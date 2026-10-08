import Link from "next/link";
import React from "react";
import { ChevronLeftIcon } from "@/icons";

interface AuthPanelProps {
	title: string;
	description: React.ReactNode;
	back: { href: string; label: string };
	children: React.ReactNode;
}

/** The form half of the signed-out screens: a way back, a heading, and what goes under it. */
export default function AuthPanel({ title, description, back, children }: AuthPanelProps) {
	return (
		<div className="flex flex-col flex-1 lg:w-1/2 w-full">
			<div className="w-full max-w-md sm:pt-10 mx-auto mb-5">
				<Link
					href={back.href}
					className="inline-flex items-center text-sm text-neutral-500 transition-colors hover:text-neutral-700 dark:text-neutral-400 dark:hover:text-neutral-300"
				>
					<ChevronLeftIcon />
					{back.label}
				</Link>
			</div>
			<div className="flex flex-col justify-center flex-1 w-full max-w-md mx-auto">
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
