"use client";
import { useAuth } from "@/context/AuthContext";
import { useRouter } from "next/navigation";
import React, { useState } from "react";
import { Dropdown } from "../ui/dropdown/Dropdown";
import { DropdownItem } from "../ui/dropdown/DropdownItem";
import { ChevronDownIcon, BoltIcon, UserCircleIcon } from "@/icons";
import { ROLE_LABELS } from "@/lib/resources/auth/auth.types";
import { cn } from "@/lib/utils/cn";

export default function UserDropdown() {
	const [isOpen, setIsOpen] = useState(false);
	const { user, logout } = useAuth();
	const router = useRouter();

	function toggleDropdown(e: React.MouseEvent<HTMLButtonElement, MouseEvent>) {
		e.stopPropagation();
		setIsOpen((prev) => !prev);
	}

	function closeDropdown() {
		setIsOpen(false);
	}

	async function handleSignOut() {
		closeDropdown();
		await logout();
		router.push("/signin");
	}

	// Initials, not a stock photo. There is no avatar upload anywhere in the
	// backend, so a real-looking portrait would be fiction.
	const initials = `${user?.firstName?.[0] ?? ""}${user?.lastName?.[0] ?? ""}`.toUpperCase();

	return (
		<div className="relative">
			<button
				onClick={toggleDropdown}
				aria-expanded={isOpen}
				aria-haspopup="menu"
				className="flex items-center gap-2 rounded-lg p-1 text-ink transition-colors hover:bg-neutral-100 dark:hover:bg-white/5"
			>
				<span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-primary-100 text-sm font-semibold text-primary-700 dark:bg-primary-500/20 dark:text-primary-200">
					{initials || "?"}
				</span>
				<span className="hidden text-sm font-medium sm:block">{user?.firstName ?? ""}</span>
				<ChevronDownIcon
					className={cn("size-4 text-ink-subtle transition-transform", isOpen && "rotate-180")}
				/>
			</button>

			<Dropdown isOpen={isOpen} onClose={closeDropdown} className="w-[260px] p-3">
				<div className="border-b border-border-default pb-3">
					<span className="block text-sm font-medium text-ink">
						{user ? `${user.firstName} ${user.lastName}` : ""}
					</span>
					<span className="mt-0.5 block truncate text-xs text-ink-subtle">
						{user?.email}
					</span>
					{user && (
						<span className="mt-2 inline-flex rounded-full bg-neutral-100 px-2 py-0.5 text-xs font-medium text-ink-muted dark:bg-white/5">
							{ROLE_LABELS[user.role]}
						</span>
					)}
				</div>

				<ul className="flex flex-col gap-1 border-b border-border-default py-3">
					<li>
						<DropdownItem
							onItemClick={closeDropdown}
							tag="a"
							href="/settings"
							className="menu-dropdown-item menu-dropdown-item-inactive"
						>
							<UserCircleIcon className="size-5 text-ink-subtle" />
							My profile
						</DropdownItem>
					</li>
					<li>
						<DropdownItem
							onItemClick={closeDropdown}
							tag="a"
							href="/settings"
							className="menu-dropdown-item menu-dropdown-item-inactive"
						>
							<BoltIcon className="size-5 text-ink-subtle" />
							Settings
						</DropdownItem>
					</li>
				</ul>

				<button
					type="button"
					onClick={handleSignOut}
					className="mt-3 flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-ink-muted transition-colors hover:bg-neutral-100 hover:text-ink dark:hover:bg-white/5"
				>
					<svg
						className="size-5"
						viewBox="0 0 24 24"
						fill="none"
						stroke="currentColor"
						strokeWidth="1.5"
						strokeLinecap="round"
						strokeLinejoin="round"
						aria-hidden="true"
					>
						<path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4M10 17l5-5-5-5M15 12H3" />
					</svg>
					Sign out
				</button>
			</Dropdown>
		</div>
	);
}
