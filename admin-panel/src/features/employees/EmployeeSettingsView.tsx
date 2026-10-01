"use client";

import { useState, type FormEvent } from "react";
import { useParams, useRouter } from "next/navigation";
import { useEmployee, useUpdateEmployee } from "@/lib/resources/employees/employees.hooks";
import type { Employee, UpdateEmployeeInput } from "@/lib/resources/employees/employees.types";
import { getErrorMessage } from "@/lib/api/errors";
import { useToast } from "@/components/ui/toast/ToastProvider";
import { Skeleton } from "@/components/ui/Skeleton";
import Card from "@/components/ui/Card";
import Button from "@/components/ui/button/Button";
import Field, { TextInput } from "@/components/ui/form/Field";
import Switch from "@/components/form/switch/Switch";

type Errors = Partial<Record<"title" | "hiredAt" | "calendarColour", string>>;

const HEX_COLOUR = /^#[0-9A-Fa-f]{6}$/;

export default function EmployeeSettingsView() {
	const params = useParams<{ id: string }>();
	const id = Number(params.id);
	const router = useRouter();
	const { toast } = useToast();

	const { data: employee, isPending } = useEmployee(Number.isNaN(id) ? null : id);
	const update = useUpdateEmployee();

	if (isPending || !employee) {
		return <Skeleton className="h-96 rounded-card" />;
	}

	async function handleSubmit(input: UpdateEmployeeInput) {
		try {
			await update.mutateAsync({ businessId: employee!.businessId, id, input });
			toast({ tone: "success", title: "Changes saved" });
		} catch (err) {
			toast({ tone: "error", title: "Couldn't save changes", description: getErrorMessage(err) });
		}
	}

	return (
		<div className="max-w-3xl">
			<EmployeeSettingsForm
				employee={employee}
				submitting={update.isPending}
				onSubmit={handleSubmit}
				onCancel={() => router.push(`/employees/${id}`)}
			/>
		</div>
	);
}

/**
 * A separate component so its field state initialises from `employee` at mount
 * time. It only ever mounts once the employee has loaded (the parent shows a
 * skeleton until then), so there is no stale-first-render to guard against.
 */
function EmployeeSettingsForm({
	employee,
	submitting,
	onSubmit,
	onCancel,
}: {
	employee: Employee;
	submitting: boolean;
	onSubmit: (input: UpdateEmployeeInput) => void | Promise<void>;
	onCancel: () => void;
}) {
	const [title, setTitle] = useState(employee.title);
	const [isActive, setIsActive] = useState(employee.isActive);
	const [hiredAt, setHiredAt] = useState(employee.hiredAt);
	const [calendarColour, setCalendarColour] = useState(employee.calendarColour ?? "");
	const [errors, setErrors] = useState<Errors>({});

	function handleSubmit(e: FormEvent) {
		e.preventDefault();
		const found: Errors = {};
		if (!title.trim()) found.title = "A job title is required.";
		if (!hiredAt) found.hiredAt = "A hire date is required.";
		if (calendarColour && !HEX_COLOUR.test(calendarColour)) {
			found.calendarColour = "Use a 6-digit hex colour, like #B76E79.";
		}
		setErrors(found);
		if (Object.keys(found).length > 0) return;

		onSubmit({
			title: title.trim(),
			isActive,
			hiredAt,
			calendarColour: calendarColour.trim() || null,
		});
	}

	return (
		<form onSubmit={handleSubmit} className="flex flex-col gap-5 md:gap-6">
			<Card title="Employment details">
				<div className="flex flex-col gap-5">
					<div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
						<Field label="Job title" required error={errors.title}>
							{(p) => (
								<TextInput
									{...p}
									value={title}
									onChange={(e) => setTitle(e.target.value)}
									placeholder="Stylist"
								/>
							)}
						</Field>
						<Field label="Hire date" required error={errors.hiredAt}>
							{(p) => (
								<TextInput
									{...p}
									type="date"
									value={hiredAt}
									onChange={(e) => setHiredAt(e.target.value)}
								/>
							)}
						</Field>
					</div>

					<Field
						label="Calendar colour"
						hint="Used once a calendar view reads it (DB-04). Leave blank for none."
						error={errors.calendarColour}
					>
						{(p) => (
							<TextInput
								{...p}
								value={calendarColour}
								onChange={(e) => setCalendarColour(e.target.value)}
								placeholder="#B76E79"
							/>
						)}
					</Field>

					<div className="flex items-center justify-between rounded-lg border border-border-default px-4 py-3">
						<div>
							<p className="text-sm font-medium text-ink">Active</p>
							<p className="text-sm text-ink-muted">
								Inactive staff stay on file but drop off the active roster.
							</p>
						</div>
						<Switch label="" defaultChecked={isActive} onChange={setIsActive} />
					</div>
				</div>
			</Card>

			<div className="flex justify-end gap-3">
				<Button type="button" variant="outline" onClick={onCancel} disabled={submitting}>
					Cancel
				</Button>
				<Button type="submit" loading={submitting}>
					Save changes
				</Button>
			</div>
		</form>
	);
}
