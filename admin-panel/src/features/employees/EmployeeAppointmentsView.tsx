"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEmployee } from "@/lib/resources/employees/employees.hooks";
import { Skeleton } from "@/components/ui/Skeleton";
import AppointmentList from "@/features/appointments/AppointmentList";
import { bookingHref } from "@/features/appointments/appointmentLinks";

/** Who a staff member is booked with, by date. */
export default function EmployeeAppointmentsView() {
	const params = useParams<{ id: string }>();
	const id = Number(params.id);

	// The detail shell has already loaded the employee; this reads the same cache entry.
	const { data: employee } = useEmployee(Number.isNaN(id) ? null : id);
	if (!employee) return <Skeleton className="h-64 rounded-card" />;

	return (
		<>
			<p className="mb-4 text-sm">
				<Link
					href={`/calendar?business=${employee.businessId}&staff=${employee.id}`}
					className="font-medium text-primary-700 hover:underline dark:text-primary-300"
				>
					See their week in the calendar
				</Link>
			</p>
			<AppointmentList
				businessId={employee.businessId}
				staffId={employee.id}
				hide={["staff"]}
				bookHref={bookingHref({ businessId: employee.businessId, staffId: employee.id })}
			/>
		</>
	);
}
