import type { Metadata } from "next";
import AppointmentDetailView from "@/features/appointments/AppointmentDetailView";
import { idParam } from "@/features/appointments/appointmentLinks";

export const metadata: Metadata = { title: "Appointment · Salon Admin" };

// `params` and `searchParams` are Promises in this version of Next and must be awaited.
export default async function AppointmentPage({
	params,
	searchParams,
}: {
	params: Promise<{ id: string }>;
	searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
	const { id } = await params;
	const { business } = await searchParams;
	return <AppointmentDetailView id={Number(id)} businessParam={idParam(business)} />;
}
