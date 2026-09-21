/**
 * Highlight a nav item for its own route *and* everything nested under it.
 *
 * The old sidebar compared `path === pathname`, so /businesses/12/services never
 * lit up "Businesses". The trailing slash in the prefix test matters: without it
 * "/service-areas" would light up "/services".
 */
export function isNavActive(
	pathname: string,
	href: string,
	match: "exact" | "prefix" = "prefix",
): boolean {
	if (match === "exact" || href === "/") return pathname === href;
	return pathname === href || pathname.startsWith(href + "/");
}
