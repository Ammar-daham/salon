import type { AuthUser } from "./auth.types";

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
