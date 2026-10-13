import type { Metadata } from "next";
import EmployeeScheduleView from "@/features/employees/EmployeeScheduleView";

export const metadata: Metadata = { title: "Schedule · Salonat Pro" };

export default function EmployeeSchedulePage() {
	return <EmployeeScheduleView />;
}
