"use client";

import { AuthUser, getCurrentUser, login as loginRequest, logout as logoutRequest } from "@/app/api/auth";
import React, { createContext, useCallback, useContext, useEffect, useState } from "react";

interface AuthContextType {
	user: AuthUser | null;
	isLoading: boolean;
	login: (email: string, password: string) => Promise<AuthUser>;
	logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
	const [user, setUser] = useState<AuthUser | null>(null);
	const [isLoading, setIsLoading] = useState(true);

	useEffect(() => {
		getCurrentUser()
			.then(setUser)
			.finally(() => setIsLoading(false));
	}, []);

	const login = useCallback(async (email: string, password: string) => {
		const loggedInUser = await loginRequest(email, password);
		setUser(loggedInUser);
		return loggedInUser;
	}, []);

	const logout = useCallback(async () => {
		await logoutRequest();
		setUser(null);
	}, []);

	return (
		<AuthContext.Provider value={{ user, isLoading, login, logout }}>
			{children}
		</AuthContext.Provider>
	);
}

export function useAuth() {
	const context = useContext(AuthContext);
	if (!context) {
		throw new Error("useAuth must be used within an AuthProvider");
	}
	return context;
}
