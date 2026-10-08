/**
 * The backend's rule for every password it sets, at sign-up, on a change or a reset: 8 to 72
 * characters, and not blank. 72 is BCrypt's input limit; anything past it would be ignored.
 */
export const PASSWORD_MIN_LENGTH = 8;
export const PASSWORD_MAX_LENGTH = 72;

/** What's wrong with a new password and its confirmation, by field. Empty when nothing is. */
export function newPasswordErrors(
	password: string,
	confirmPassword: string,
): { password?: string; confirmPassword?: string } {
	const errors: { password?: string; confirmPassword?: string } = {};
	if (!password.trim()) errors.password = "Password is required.";
	else if (password.length < PASSWORD_MIN_LENGTH) errors.password = `Use at least ${PASSWORD_MIN_LENGTH} characters.`;
	else if (password.length > PASSWORD_MAX_LENGTH) errors.password = `Use at most ${PASSWORD_MAX_LENGTH} characters.`;
	if (password !== confirmPassword) errors.confirmPassword = "Passwords don't match.";
	return errors;
}
