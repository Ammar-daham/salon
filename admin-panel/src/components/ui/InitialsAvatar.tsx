import { cn } from "@/lib/utils/cn";

const sizes = {
	sm: "size-8 text-xs",
	md: "size-10 text-sm",
	lg: "size-14 text-lg",
} as const;

/**
 * Initials rather than a portrait. There is no avatar upload anywhere in the
 * backend, so a photo would be fiction — and stock faces on a staff roster read
 * as real people who don't exist.
 */
export default function InitialsAvatar({
	firstName,
	lastName,
	size = "md",
	muted = false,
	className,
}: {
	firstName?: string;
	lastName?: string;
	size?: keyof typeof sizes;
	muted?: boolean;
	className?: string;
}) {
	const initials =
		`${firstName?.[0] ?? ""}${lastName?.[0] ?? ""}`.toUpperCase() || "?";

	return (
		<span
			aria-hidden="true"
			className={cn(
				"flex shrink-0 items-center justify-center rounded-full font-semibold",
				sizes[size],
				muted
					? "bg-neutral-100 text-ink-muted dark:bg-white/5 dark:text-neutral-300"
					: "bg-primary-100 text-primary-700 dark:bg-primary-500/20 dark:text-primary-200",
				className,
			)}
		>
			{initials}
		</span>
	);
}
