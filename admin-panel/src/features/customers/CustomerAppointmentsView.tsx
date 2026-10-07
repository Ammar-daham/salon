"use client";

import { useParams } from "next/navigation";
import { useCustomer } from "@/lib/resources/customers/customers.hooks";
import { Skeleton } from "@/components/ui/Skeleton";
import AppointmentList from "@/features/appointments/AppointmentList";
import { bookingHref } from "@/features/appointments/appointmentLinks";

/** Every appointment a client has had or has coming, newest first. */
export default function CustomerAppointmentsView() {
	const params = useParams<{ id: string }>();
	const id = Number(params.id);
	// The shell above says when the client isn't found.
	const { data: customer } = useCustomer(Number.isNaN(id) ? null : id);
	if (!customer) return <Skeleton className="h-64 rounded-card" />;

	return (
		<AppointmentList
			businessId={customer.businessId}
			customerId={customer.id}
			mode="history"
			hide={["customer"]}
			emptyHint="Their past and upcoming bookings will show here."
			bookHref={bookingHref({ businessId: customer.businessId, customerId: customer.id })}
		/>
	);
}
