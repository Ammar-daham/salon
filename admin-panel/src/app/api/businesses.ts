import { api } from "./axios";

export interface Business {
	id: number;
	name: string;
	status: "PENDING" | "APPROVED" | "REJECTED" | "SUSPENDED";
}

export async function getBusinesses(): Promise<Business[]> {
	const { data } = await api.get<Business[]>("/api/v1/business");
	return data;
}
