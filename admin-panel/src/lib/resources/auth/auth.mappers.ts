import type { AuthUser, ChangePasswordInput, ResetPasswordInput } from "./auth.types";

export interface AuthUserDto {
	id: number;
	first_name: string;
	last_name: string;
	email: string;
	role: AuthUser["role"];
	business_id: number | null;
}

export function toAuthUser(dto: AuthUserDto): AuthUser {
	return {
		id: dto.id,
		firstName: dto.first_name,
		lastName: dto.last_name,
		email: dto.email,
		role: dto.role,
		businessId: dto.business_id,
	};
}

export function toChangePasswordRequest(input: ChangePasswordInput) {
	return {
		current_password: input.currentPassword,
		new_password: input.newPassword,
	};
}

/** The new password is `password` here, where a change has `new_password`. */
export function toResetPasswordRequest(input: ResetPasswordInput) {
	return {
		token: input.token,
		password: input.password,
	};
}
