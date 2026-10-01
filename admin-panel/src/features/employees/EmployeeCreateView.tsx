"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { scopedBusinessId } from "@/lib/auth/scope";
import { useCreateUser } from "@/lib/resources/users/users.hooks";
import { useCreateEmployee } from "@/lib/resources/employees/employees.hooks";
import type { CreateUserInput } from "@/lib/resources/users/users.types";
import { getErrorMessage, normalizeError } from "@/lib/api/errors";
import PageHeader from "@/components/ui/PageHeader";
import Card from "@/components/ui/Card";
import Field, { TextInput } from "@/components/ui/form/Field";
import { useToast } from "@/components/ui/toast/ToastProvider";
import UserForm from "@/features/users/UserForm";

type ExtraErrors = Partial<Record<"title" | "hiredAt", string>>;

function todayIsoDate() {
	const now = new Date();
	return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}

/**
 * POST /users creates the account; POST .../staff creates the roster entry that
 * backs the Employees list. Before BE-08/BE-35, only the first half existed, so
 * "Add employee" silently produced someone who could sign in but would never
 * appear on the team roster (FE-03). This view does both, in order.
 */
export default function EmployeeCreateView() {
	const router = useRouter();
	const { user: currentUser } = useAuth();
	const { toast } = useToast();
	const createUser = useCreateUser();
	const createEmployee = useCreateEmployee();

	const [title, setTitle] = useState("");
	const [hiredAt, setHiredAt] = useState(todayIsoDate);
	const [errors, setErrors] = useState<ExtraErrors>({});

	async function handleSubmit(input: CreateUserInput) {
		const found: ExtraErrors = {};
		if (!title.trim()) found.title = "A job title is required.";
		if (!hiredAt) found.hiredAt = "A hire date is required.";
		setErrors(found);
		if (Object.keys(found).length > 0) return;

		// SUPER_ADMIN picked a salon in the form above (input.businessId); anyone
		// else is scoped to their own, the same business the account itself lands in.
		const businessId = input.businessId ?? scopedBusinessId(currentUser);
		if (businessId == null) {
			toast({
				tone: "error",
				title: "Couldn't create account",
				description: "No salon is selected.",
			});
			return;
		}

		let createdUser;
		try {
			createdUser = await createUser.mutateAsync(input);
		} catch (err) {
			const apiError = normalizeError(err);
			toast({
				tone: "error",
				title: apiError.kind === "conflict" ? "That email is already in use" : "Couldn't create account",
				description: getErrorMessage(err),
			});
			return;
		}

		try {
			await createEmployee.mutateAsync({
				businessId,
				input: { userId: createdUser.id, title: title.trim(), isActive: true, hiredAt },
			});
		} catch (err) {
			// The account is real at this point - don't imply the whole thing failed.
			toast({
				tone: "error",
				title: "Account created, but the roster entry failed",
				description: `${createdUser.firstName} ${createdUser.lastName} can sign in, but isn't on the team roster yet. ${getErrorMessage(err)}`,
			});
			router.push("/employees");
			return;
		}

		toast({
			tone: "success",
			title: "Employee added",
			description: `${createdUser.firstName} ${createdUser.lastName} can now sign in and is on the team roster.`,
		});
		router.push("/employees");
	}

	return (
		<>
			<PageHeader title="Add employee" description="Create a staff account for your salon." />
			<div className="max-w-3xl">
				<UserForm
					mode="create"
					allowedRoles={["EMPLOYEE", "ADMIN"]}
					submitting={createUser.isPending || createEmployee.isPending}
					onSubmitCreate={handleSubmit}
					onCancel={() => router.push("/employees")}
					extraSection={
						<Card title="Employment details" description="Shown on the team roster.">
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
						</Card>
					}
				/>
			</div>
		</>
	);
}
