"use client";

import AppointmentList from "@/features/appointments/AppointmentList";
import { bookingHref } from "@/features/appointments/appointmentLinks";
import { useBusinessId } from "./BusinessDetailShell";

/** The salon's bookings, by date. */
export default function BusinessAppointmentsView() {
	const businessId = useBusinessId();
	return <AppointmentList businessId={businessId} bookHref={bookingHref({ businessId })} />;
}
