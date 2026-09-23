import type { Id } from "@/lib/api/types";
import type { Employee, WorkingDay } from "@/lib/resources/employees/employees.types";
import type { Customer } from "@/lib/resources/customers/customers.types";
import {
	CUSTOMER_NOTES,
	CUSTOMER_TAGS,
	FIRST_NAMES,
	LAST_NAMES,
	STAFF_TITLES,
	WEEKDAYS,
} from "./pools";
import { daysAgo, hashSeed, intBetween, makeRng, pick, pickMany, type Rng } from "./random";

/**
 * Generators are seeded from the salon's own id, so a salon's team and client
 * list are identical on every render and across reloads — and two salons never
 * look like copies of each other.
 *
 * Mock ids are offset per salon so they stay unique across a platform-wide list
 * without ever colliding with a real database id in a URL.
 */
const EMPLOYEE_ID_BASE = 900_000;
const CUSTOMER_ID_BASE = 800_000;

function person(rng: Rng) {
	return { firstName: pick(rng, FIRST_NAMES), lastName: pick(rng, LAST_NAMES) };
}

function slugEmail(first: string, last: string, domain: string, salt: number) {
	const strip = (s: string) =>
		s.toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/[^a-z]/g, "");
	return `${strip(first)}.${strip(last)}${salt}@${domain}`;
}

function workingHours(rng: Rng): WorkingDay[] {
	// Two days off, at least one of them at the weekend.
	const offDays = new Set(pickMany(rng, WEEKDAYS, 2));
	return WEEKDAYS.map((day) => {
		if (offDays.has(day)) return { day, start: null, end: null };
		const start = pick(rng, ["08:00", "09:00", "10:00", "11:00"]);
		const end = pick(rng, ["16:00", "17:00", "18:00", "19:00"]);
		return { day, start, end };
	});
}

export function generateEmployees(businessId: Id, businessName: string): Employee[] {
	const rng = makeRng(hashSeed("employees", businessId, businessName));
	const count = intBetween(rng, 3, 7);
	const domain = `${businessName.toLowerCase().replace(/[^a-z]/g, "").slice(0, 12) || "salon"}.test`;

	return Array.from({ length: count }, (_, i) => {
		const { firstName, lastName } = person(rng);
		return {
			id: EMPLOYEE_ID_BASE + Number(businessId) * 100 + i,
			firstName,
			lastName,
			email: slugEmail(firstName, lastName, domain, i + 1),
			title: pick(rng, STAFF_TITLES),
			// Roughly one in eight is on leave or no longer active.
			isActive: rng() > 0.125,
			businessId,
			businessName,
			hiredAt: daysAgo(rng, 1400),
			workingHours: workingHours(rng),
			appointmentsThisWeek: intBetween(rng, 0, 24),
		};
	});
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
