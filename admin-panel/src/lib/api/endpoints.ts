/**
 * Every backend URL lives here. No path string literals anywhere else.
 *
 * Note the singular `business` segment — that is the backend's actual path, not
 * a typo. PATCH is NOT in the server's CORS allowed methods, so nothing here
 * may use it.
 */
export const endpoints = {
	auth: {
		login: "/api/v1/auth/login",
		logout: "/api/v1/auth/logout",
		me: "/api/v1/auth/me",
	},
	users: {
		root: "/api/v1/users",
		byId: (id: number) => `/api/v1/users/${id}`,
	},
	businesses: {
		root: "/api/v1/business",
		byId: (id: number) => `/api/v1/business/${id}`,
		// There is no list endpoint for services: read business.services instead.
		services: (businessId: number) => `/api/v1/business/${businessId}/services`,
		serviceById: (businessId: number, serviceId: number) =>
			`/api/v1/business/${businessId}/services/${serviceId}`,
	},
	addresses: {
		root: "/api/v1/addresses",
		byId: (id: number) => `/api/v1/addresses/${id}`,
	},
	contacts: {
		root: "/api/v1/contacts",
		byId: (id: number) => `/api/v1/contacts/${id}`,
	},
} as const;
