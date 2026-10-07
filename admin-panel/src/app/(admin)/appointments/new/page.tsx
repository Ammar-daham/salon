import type { Metadata } from "next";
import AppointmentCreateView from "@/features/appointments/AppointmentCreateView";
import { dateParam, idParam } from "@/features/appointments/appointmentLinks";

export const metadata: Metadata = { title: "Book appointment · Salon Admin" };

// `searchParams` is a Promise in this version of Next and must be awaited.
export default async function NewAppointmentPage({
	searchParams,
}: {
	searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
	const { business, customer, staff, date } = await searchParams;
	return (
		<AppointmentCreateView
			businessParam={idParam(business)}
			customerParam={idParam(customer)}
			staffParam={idParam(staff)}
			dateParam={dateParam(date)}
		/>
	);
}
