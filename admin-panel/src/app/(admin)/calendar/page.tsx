import type { Metadata } from "next";
import CalendarView from "@/features/calendar/CalendarView";
import { idParam } from "@/features/appointments/appointmentLinks";

export const metadata: Metadata = {
	title: "Calendar · Salon Admin",
};

// `searchParams` is a Promise in this version of Next and must be awaited.
export default async function CalendarPage({
	searchParams,
}: {
	searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
	const { business, staff } = await searchParams;
	return <CalendarView businessParam={idParam(business)} staffParam={idParam(staff)} />;
}
