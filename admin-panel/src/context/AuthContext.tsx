"use client";

import React, { createContext, useCallback, useContext, useEffect, useState } from "react";
import { setUnauthorizedHandler } from "@/lib/api/client";
import { canUseAdminPanel } from "@/lib/auth/permissions";
import {
	getCurrentUser,
	login as loginRequest,
	logout as logoutRequest,
} from "@/lib/resources/auth/auth.api";
import type { AuthUser } from "@/lib/resources/auth/auth.types";

interface AuthContextType {
	user: AuthUser | null;
	isLoading: boolean;
	login: (email: string, password: string) => Promise<AuthUser>;
	logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const CUSTOMER_REFUSED = "Customer accounts can't sign in to the admin panel.";

/** Ends a session the panel won't use. If that fails, it's left to expire. */
async function endSession(): Promise<void> {
	await logoutRequest().catch(() => undefined);
}

/**
 * Session identity stays a context rather than a query-cache entry: the 401
 * interceptor has to be able to clear it synchronously, before any dependent
 * render happens.
 */
export function AuthProvider({ children }: { children: React.ReactNode }) {
	const [user, setUser] = useState<AuthUser | null>(null);
	const [isLoading, setIsLoading] = useState(true);

	useEffect(() => {
		let cancelled = false;
		getCurrentUser()
			.then(async (u) => {
				// A customer's session, e.g. one whose role was changed while signed in.
				const usable = canUseAdminPanel(u) ? u : null;
				if (u && !usable) await endSession();
				if (!cancelled) setUser(usable);
			})
			.catch(() => {
				if (!cancelled) setUser(null);
			})
			.finally(() => {
				if (!cancelled) setIsLoading(false);
			});
		return () => {
			cancelled = true;
		};
	}, []);

	// Any 401 from a request that isn't the boot probe or the login form means
	// the session lapsed mid-use. Drop the user; the admin layout redirects.
	useEffect(() => {
		setUnauthorizedHandler(() => setUser(null));
		return () => setUnauthorizedHandler(null);
	}, []);

	const login = useCallback(async (email: string, password: string) => {
		const loggedInUser = await loginRequest(email, password);
		if (!canUseAdminPanel(loggedInUser)) {
			await endSession();
			throw new Error(CUSTOMER_REFUSED);
		}
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
