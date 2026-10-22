import { apiClient } from "@/lib/api/client";
import { endpoints } from "@/lib/api/endpoints";
import type { RegistrationInput } from "./registration.types";
import { toRegistrationRequest } from "./registration.mappers";

/**
 * Registers the salon and its owner, with no one signed in. The salon waits for a super admin to
 * approve it, and the owner can sign in only then. The answer is a 202 with no body.
 */
export async function registerBusiness(input: RegistrationInput): Promise<void> {
	await apiClient.post(endpoints.auth.register, toRegistrationRequest(input));
}
