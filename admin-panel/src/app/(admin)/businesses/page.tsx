import type { Metadata } from "next";
import BusinessListView from "@/features/businesses/BusinessListView";
import { businessStatusParam } from "@/lib/resources/businesses/businesses.types";

export const metadata: Metadata = {
	title: "Businesses · Salonat Pro",
	description: "Every salon on the platform.",
};

// `searchParams` is a Promise in this version of Next and must be awaited.
export default async function BusinessesPage({
	searchParams,
}: {
	searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
	const { status } = await searchParams;
	return <BusinessListView initialStatus={businessStatusParam(status)} />;
}
