/**
 * Deterministic pseudo-randomness.
 *
 * Mock data must be stable: the same salon has to show the same team and the
 * same clients on every render, every reload and in every tab. Anything derived
 * from Math.random() would reshuffle mid-session and read as a bug.
 *
 * mulberry32 — small, fast, good enough distribution for fixture data.
 */
export function makeRng(seed: number) {
	let state = seed >>> 0;
	return function next(): number {
		state = (state + 0x6d2b79f5) >>> 0;
		let t = state;
		t = Math.imul(t ^ (t >>> 15), t | 1);
		t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
		return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
	};
}

export type Rng = ReturnType<typeof makeRng>;

/** Stable 32-bit hash so a string can seed the generator. */
export function hashSeed(...parts: (string | number)[]): number {
	let h = 2166136261;
	for (const part of parts.join("|")) {
		h ^= part.charCodeAt(0);
		h = Math.imul(h, 16777619);
	}
	return h >>> 0;
}

export function pick<T>(rng: Rng, items: readonly T[]): T {
	return items[Math.floor(rng() * items.length)];
}

export function pickMany<T>(rng: Rng, items: readonly T[], count: number): T[] {
	const pool = [...items];
	const out: T[] = [];
	for (let i = 0; i < count && pool.length > 0; i++) {
		out.push(pool.splice(Math.floor(rng() * pool.length), 1)[0]);
	}
	return out;
}

export function intBetween(rng: Rng, min: number, max: number): number {
	return min + Math.floor(rng() * (max - min + 1));
}

/** A date `daysAgo` in the past, with a stable time-of-day. */
export function daysAgo(rng: Rng, maxDays: number): string {
	const days = intBetween(rng, 0, maxDays);
	const d = new Date();
	d.setHours(intBetween(rng, 9, 18), pick(rng, [0, 15, 30, 45]), 0, 0);
	d.setDate(d.getDate() - days);
	return d.toISOString();
}
