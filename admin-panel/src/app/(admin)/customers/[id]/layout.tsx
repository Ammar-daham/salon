import React from "react";
import CustomerDetailShell from "@/features/customers/CustomerDetailShell";

// `params` is a Promise in this version of Next and must be awaited.
export default async function CustomerDetailLayout({
	children,
	params,
}: {
	children: React.ReactNode;
	params: Promise<{ id: string }>;
}) {
	const { id } = await params;
	return <CustomerDetailShell id={Number(id)}>{children}</CustomerDetailShell>;
}
