/**
 * Every price belongs to a salon, and each salon has one ISO 4217 currency
 * (businesses.currency, DB-08). Format with that code, never a hard-coded one.
 */
export function formatMoney(value: number, currency: string): string {
	return new Intl.NumberFormat(undefined, { style: "currency", currency }).format(value);
}

/** The currencies offered in the salon form. A salon already using another code keeps it. */
export const CURRENCY_OPTIONS = ["EUR", "SEK", "NOK", "DKK", "GBP", "CHF", "PLN", "USD"] as const;
