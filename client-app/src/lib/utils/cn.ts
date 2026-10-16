import { twMerge } from "tailwind-merge";

type ClassValue = string | number | null | undefined | false | ClassValue[];

function flatten(input: ClassValue): string {
	if (!input) return "";
	if (Array.isArray(input)) return input.map(flatten).filter(Boolean).join(" ");
	return String(input);
}

/**
 * Join conditional class names, with later Tailwind utilities winning over
 * earlier conflicting ones. Every primitive takes a `className` prop that is
 * merged through here, so callers can always override a default.
 */
export function cn(...inputs: ClassValue[]): string {
	return twMerge(inputs.map(flatten).filter(Boolean).join(" "));
}
