import type { Metadata } from "next";
import AppointmentEditView from "@/features/appointments/AppointmentEditView";
import { idParam } from "@/features/appointments/appointmentLinks";

export const metadata: Metadata = { title: "Edit appointment · Salon Admin" };

// `params` and `searchParams` are Promises in this version of Next and must be awaited.
export default async function EditAppointmentPage({
	params,
	searchParams,
}: {
	params: Promise<{ id: string }>;
	searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
	const { id } = await params;
	const { business } = await searchParams;
	return <AppointmentEditView id={Number(id)} businessParam={idParam(business)} />;
}
