import React from "react";
import EmployeeDetailShell from "@/features/employees/EmployeeDetailShell";

// `params` is a Promise in this version of Next and must be awaited.
export default async function EmployeeDetailLayout({
	children,
	params,
}: {
	children: React.ReactNode;
	params: Promise<{ id: string }>;
}) {
	const { id } = await params;
	return <EmployeeDetailShell id={Number(id)}>{children}</EmployeeDetailShell>;
}
