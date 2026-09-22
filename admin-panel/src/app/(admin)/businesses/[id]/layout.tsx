import React from "react";
import BusinessDetailShell from "@/features/businesses/BusinessDetailShell";

// `params` is a Promise in this version of Next and must be awaited.
export default async function BusinessDetailLayout({
	children,
	params,
}: {
	children: React.ReactNode;
	params: Promise<{ id: string }>;
}) {
	const { id } = await params;
	return <BusinessDetailShell id={Number(id)}>{children}</BusinessDetailShell>;
}
