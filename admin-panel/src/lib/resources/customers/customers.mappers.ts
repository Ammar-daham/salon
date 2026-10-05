import type { Id } from "@/lib/api/types";
import type { Customer, CustomerInput } from "./customers.types";

export interface CustomerDto {
	id: number;
	user_id: number | null;
	first_name: string;
	last_name: string;
	email: string | null;
	phone: string | null;
	notes: string | null;
	marketing_consent: boolean;
	created_at: string;
	updated_at: string | null;
}

interface CustomerRequestDto {
	first_name: string;
	last_name: string;
	email: string | null;
	phone: string | null;
	notes: string | null;
	marketing_consent: boolean;
}

/** The customer response doesn't carry its salon (business_id is server-side only), so the caller supplies it. */
export function toCustomer(dto: CustomerDto, businessId: Id, businessName: string): Customer {
	return {
		id: dto.id,
		firstName: dto.first_name,
		lastName: dto.last_name,
		email: dto.email,
		phone: dto.phone,
		notes: dto.notes,
		marketingConsent: dto.marketing_consent,
		userId: dto.user_id,
		businessId,
		businessName,
		createdAt: dto.created_at,
		updatedAt: dto.updated_at,
	};
}

function blankToNull(value: string | null) {
	const trimmed = value?.trim() ?? "";
	return trimmed === "" ? null : trimmed;
}

export function toCustomerRequest(input: CustomerInput): CustomerRequestDto {
	return {
		first_name: input.firstName.trim(),
		last_name: input.lastName.trim(),
		email: blankToNull(input.email),
		phone: blankToNull(input.phone),
		notes: blankToNull(input.notes),
		marketing_consent: input.marketingConsent,
	};
}

/** The full-replace PUT body for an existing record, with some fields changed. */
export function customerInputFrom(customer: Customer, changes: Partial<CustomerInput> = {}): CustomerInput {
	return {
		firstName: customer.firstName,
		lastName: customer.lastName,
		email: customer.email,
		phone: customer.phone,
		notes: customer.notes,
		marketingConsent: customer.marketingConsent,
		...changes,
	};
}
