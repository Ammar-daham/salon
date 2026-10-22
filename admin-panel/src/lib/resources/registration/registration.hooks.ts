"use client";

import { useMutation } from "@tanstack/react-query";
import { registerBusiness } from "./registration.api";
import type { RegistrationInput } from "./registration.types";

export function useRegisterBusiness() {
	return useMutation({
		mutationFn: (input: RegistrationInput) => registerBusiness(input),
	});
}
