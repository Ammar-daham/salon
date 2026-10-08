"use client";

import { useMutation } from "@tanstack/react-query";
import { changePassword, requestPasswordReset, resetPassword } from "./auth.api";
import type { ChangePasswordInput, ResetPasswordInput } from "./auth.types";

// Nothing cached depends on a password, so none of these invalidate anything.

export function useChangePassword() {
	return useMutation({
		mutationFn: (input: ChangePasswordInput) => changePassword(input),
	});
}

export function useRequestPasswordReset() {
	return useMutation({
		mutationFn: (email: string) => requestPasswordReset(email),
	});
}

export function useResetPassword() {
	return useMutation({
		mutationFn: (input: ResetPasswordInput) => resetPassword(input),
	});
}
