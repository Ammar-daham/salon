"use client";

import { useMutation } from "@tanstack/react-query";
import { changePassword } from "./auth.api";
import type { ChangePasswordInput } from "./auth.types";

/** Nothing cached depends on a password, so there is nothing to invalidate. */
export function useChangePassword() {
	return useMutation({
		mutationFn: (input: ChangePasswordInput) => changePassword(input),
	});
}
