"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";
import { isApiError } from "@/lib/api/errors";

/**
 * Caching is architectural here, not an optimisation. The backend has no
 * pagination, and `GET /business` fans out into per-business address, contact
 * and service queries — so refetching on every mount turns one page view into a
 * query storm. A shared cache with a stale window is what makes the UI viable.
 */
export default function QueryProvider({ children }: { children: React.ReactNode }) {
	const [client] = useState(
		() =>
			new QueryClient({
				defaultOptions: {
					queries: {
						staleTime: 60_000,
						refetchOnWindowFocus: false,
						retry: (failureCount, error) => {
							// Never retry an auth/permission/validation failure — retrying a
							// 401 just triples the redirect race.
							if (
								isApiError(error) &&
								["unauthorized", "forbidden", "not-found", "validation", "conflict"].includes(
									error.kind,
								)
							) {
								return false;
							}
							return failureCount < 2;
						},
					},
					mutations: { retry: false },
				},
			}),
	);

	return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}
