/**
 * Every backend URL lives here. No path string literals anywhere else.
 *
 * PATCH is NOT in the server's CORS allowed methods, so nothing here may use it.
 */
export const endpoints = {
	auth: {
		login: "/api/v1/auth/login",
		logout: "/api/v1/auth/logout",
		me: "/api/v1/auth/me",
		changePassword: "/api/v1/auth/change-password",
		// Both open to anyone, signed in or not.
		forgotPassword: "/api/v1/auth/forgot-password",
		resetPassword: "/api/v1/auth/reset-password",
	},
	users: {
		root: "/api/v1/users",
		byId: (id: number) => `/api/v1/users/${id}`,
	},
	businesses: {
		root: "/api/v1/businesses",
		byId: (id: number) => `/api/v1/businesses/${id}`,
		// There is no list endpoint for services: read business.services instead.
		services: (businessId: number) => `/api/v1/businesses/${businessId}/services`,
		serviceById: (businessId: number, serviceId: number) =>
			`/api/v1/businesses/${businessId}/services/${serviceId}`,
		// GET and PUT only: the week is always read and replaced whole.
		hours: (businessId: number) => `/api/v1/businesses/${businessId}/hours`,
		staff: (businessId: number) => `/api/v1/businesses/${businessId}/staff`,
		staffById: (businessId: number, staffId: number) =>
			`/api/v1/businesses/${businessId}/staff/${staffId}`,
		// GET and PUT only, like the salon's hours.
		staffSchedule: (businessId: number, staffId: number) =>
			`/api/v1/businesses/${businessId}/staff/${staffId}/schedule`,
		customers: (businessId: number) => `/api/v1/businesses/${businessId}/customers`,
		customerById: (businessId: number, customerId: number) =>
			`/api/v1/businesses/${businessId}/customers/${customerId}`,
		// No DELETE: an appointment is cancelled through its status instead.
		appointments: (businessId: number) => `/api/v1/businesses/${businessId}/appointments`,
		appointmentById: (businessId: number, appointmentId: number) =>
			`/api/v1/businesses/${businessId}/appointments/${appointmentId}`,
		appointmentStatus: (businessId: number, appointmentId: number) =>
			`/api/v1/businesses/${businessId}/appointments/${appointmentId}/status`,
		availability: (businessId: number, serviceId: number) =>
			`/api/v1/businesses/${businessId}/services/${serviceId}/availability`,
	},
	addresses: {
		root: "/api/v1/addresses",
		byId: (id: number) => `/api/v1/addresses/${id}`,
	},
	contacts: {
		root: "/api/v1/contacts",
		byId: (id: number) => `/api/v1/contacts/${id}`,
	},
	// Across salons: a SUPER_ADMIN's are every salon's, anyone else's only their own salon's.
	customers: {
		root: "/api/v1/customers",
		byId: (id: number) => `/api/v1/customers/${id}`,
	},
} as const;
