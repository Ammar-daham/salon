/** What the backend gives a salon created without one; every existing salon is in Germany. */
export const DEFAULT_TIME_ZONE = "Europe/Berlin";

/**
 * The IANA zones the browser knows, for the salon form, with `current` kept in even if the
 * browser doesn't list it. The backend refuses fixed offsets such as "+01:00", which ignore
 * daylight saving time, and the browser's list has none.
 */
export function timeZoneOptions(current: string): string[] {
	const known = typeof Intl.supportedValuesOf === "function" ? Intl.supportedValuesOf("timeZone") : [];
	return [...new Set([current, ...known])];
}
