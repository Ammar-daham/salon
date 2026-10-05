import type { Id } from "@/lib/api/types";
import type { Customer } from "@/lib/resources/customers/customers.types";
import { CUSTOMER_NOTES, CUSTOMER_TAGS, FIRST_NAMES, LAST_NAMES } from "./pools";
import { daysAgo, hashSeed, intBetween, makeRng, pick, pickMany, type Rng } from "./random";

/**
 * Generators are seeded from the salon's own id, so a salon's client list is
 * identical on every render and across reloads — and two salons never look
 * like copies of each other.
 *
 * Mock ids are offset per salon so they stay unique across a platform-wide list
 * without ever colliding with a real database id in a URL.
 *
 * Employees no longer have a generator here - staff is a real, business-scoped
 * API now (BE-08/BE-35/DB-04). See employees.api.ts.
 */
const CUSTOMER_ID_BASE = 800_000;

function person(rng: Rng) {
	return { firstName: pick(rng, FIRST_NAMES), lastName: pick(rng, LAST_NAMES) };
}

function slugEmail(first: string, last: string, domain: string, salt: number) {
	const strip = (s: string) =>
		s.toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/[^a-z]/g, "");
	return `${strip(first)}.${strip(last)}${salt}@${domain}`;
}

export function generateCustomers(businessId: Id, businessName: string): Customer[] {
	const rng = makeRng(hashSeed("customers", businessId, businessName));
	const count = intBetween(rng, 18, 40);

	return Array.from({ length: count }, (_, i) => {
		const { firstName, lastName } = person(rng);
		const totalVisits = intBetween(rng, 1, 32);
		const first = daysAgo(rng, 900);
		const last = daysAgo(rng, 120);

		return {
			id: CUSTOMER_ID_BASE + Number(businessId) * 100 + i,
			firstName,
			lastName,
			email: slugEmail(firstName, lastName, "example.test", i + 1),
			phone: `+358 4${intBetween(rng, 0, 9)} ${intBetween(rng, 100, 999)} ${intBetween(rng, 1000, 9999)}`,
			businessId,
			businessName,
			tags: pickMany(rng, CUSTOMER_TAGS, intBetween(rng, 0, 2)),
			notes: rng() > 0.45 ? pick(rng, CUSTOMER_NOTES) : null,
			firstVisit: first < last ? first : last,
			lastVisit: first < last ? last : first,
			totalVisits,
			// Average ticket between roughly 40 and 120 EUR.
			totalSpend: Math.round(totalVisits * intBetween(rng, 40, 120) * 100) / 100,
		};
	});
}
