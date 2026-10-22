/**
 * The Salonat API: every endpoint the admin panel, Salonat Pro, and any other client can call. Each
 * controller here is one resource, and each of its methods documents one endpoint: its path, who may
 * call it, what it answers and when it refuses.
 *
 * <h2>Basics</h2>
 * <ul>
 *   <li>Every path starts with {@code /api/v1}. Bodies are JSON, with snake_case field names such as
 *       {@code first_name}. A field the endpoint doesn't take is ignored.</li>
 *   <li>A {@code POST} that creates a record answers 201 with the record as stored, and its address in
 *       {@code Location}. A {@code PUT} answers 200 with the record as stored, and a {@code DELETE} answers
 *       204, with no body.</li>
 *   <li>Opening hours, shifts, time off and appointments are on the salon's clock: wall-clock times,
 *       such as {@code 2026-10-14T10:30}, in the salon's {@code timezone}, with no offset. Record
 *       timestamps such as {@code created_at} are instants in UTC, such as {@code 2026-10-14T08:30:00Z}.</li>
 *   <li>Money is a decimal number with at most two places, in the salon's {@code currency}.</li>
 *   <li>Every response carries an {@code X-Request-Id} header, and every log line the request wrote
 *       carries the same id, so quote it when reporting a problem. A client may send its own: up to 64
 *       letters, digits, dots, dashes and underscores. Anything else is replaced.</li>
 * </ul>
 *
 * <h2>Signing in</h2>
 * <p>{@code POST /api/v1/auth/login} with an email and password starts a session, kept in the
 * {@code JSESSIONID} cookie, which the client sends with every request. Every endpoint needs a session
 * except {@code POST /api/v1/auth/login}, {@code POST /api/v1/auth/forgot-password},
 * {@code POST /api/v1/auth/reset-password} and {@code GET /actuator/health}; without one the answer is
 * 401. A salon's ADMINs and EMPLOYEEs sign in only once a SUPER_ADMIN has approved the salon, and are
 * signed out if it goes back to pending or is rejected; a suspended salon's staff still sign in. A
 * session ends after 30 minutes without a request. A request whose {@code Origin} header isn't
 * one the server allows ({@code app.cors.allowed-origins}) is refused with 403, which is why there is no
 * CSRF token.
 *
 * <h2>Roles</h2>
 * <table>
 *   <caption>Who a signed-in user is</caption>
 *   <tr><th>Role</th><th>Who</th></tr>
 *   <tr><td>{@code SUPER_ADMIN}</td><td>Runs the platform: every salon and every user.</td></tr>
 *   <tr><td>{@code ADMIN}</td><td>Runs one salon, their {@code business_id}: its staff, services, hours,
 *       clients and appointments.</td></tr>
 *   <tr><td>{@code EMPLOYEE}</td><td>Works at one salon: sees it, adds its clients, and books and changes
 *       its appointments.</td></tr>
 *   <tr><td>{@code CUSTOMER}</td><td>A salon's client with an account. Does no salon work. Can sign in if
 *       the account has an email and password, for the customer app; the admin panel turns them away.</td></tr>
 * </table>
 * <p>Everyone may read and change their own account and password. An {@code ADMIN} or {@code EMPLOYEE}
 * acts only on their own salon. Each endpoint says who may call it; anyone else gets 403, or 404 where
 * the record isn't theirs to see.
 *
 * <h2>Lists</h2>
 * <p>Salons, users, customers, appointments and notifications come a page at a time:
 * {@code {"items": [...], "page": 1, "size": 20, "total_items": 57, "total_pages": 3}}. {@code ?page}
 * counts from 1 and {@code ?size} is 1 to 100, 20 if left out; a page past the end has no items.
 * {@code ?sort} is one of the list's keys, or {@code -key} for the other way round, and {@code ?q}
 * searches every list but appointments. Other lists, which a salon or a date range keeps short, come
 * whole.
 *
 * <h2>Errors</h2>
 * <p>An error has a status and a body such as
 * {@code {"errorCode": "NOT_FOUND", "message": "Appointment with id 42 not found.", "status": "NOT_FOUND",
 * "timestamp": "2026-10-14T08:30:00Z", "path": "/api/v1/businesses/1/appointments/42"}}. Unlike every other body, its
 * fields are camelCase. {@code message} is meant for people; act on the status or {@code errorCode}.
 * <table>
 *   <caption>Error codes</caption>
 *   <tr><th>Status</th><th>{@code errorCode}</th><th>When</th></tr>
 *   <tr><td>400</td><td>{@code BAD_REQUEST}</td><td>The request can't be done as sent: a field that fails
 *       its rules, a malformed body or parameter, or a value the database refuses. The message says
 *       what's wrong, naming each field as the body does, such as
 *       {@code addresses[0].postal_code size must be between 0 and 20}.</td></tr>
 *   <tr><td>401</td><td>{@code UNAUTHORIZED}</td><td>Not signed in, or the email and password don't
 *       match.</td></tr>
 *   <tr><td>403</td><td>{@code FORBIDDEN}</td><td>Signed in, but not allowed to do this.</td></tr>
 *   <tr><td>404</td><td>{@code NOT_FOUND}</td><td>No such record, or not one the caller may see.</td></tr>
 *   <tr><td>409</td><td>{@code CONFLICT}</td><td>It clashes with what's stored: a duplicate, a double
 *       booking, or a change the record no longer allows, such as to a cancelled appointment.</td></tr>
 *   <tr><td>429</td><td>{@code TOO_MANY_REQUESTS}</td><td>Too many failed sign-ins. {@code Retry-After}
 *       says how many seconds to wait.</td></tr>
 *   <tr><td>500</td><td>{@code INTERNAL_ERROR}, {@code DATABASE_ERROR}</td><td>A fault on the server,
 *       logged with the request id.</td></tr>
 * </table>
 *
 * <h2>Resources</h2>
 * <ul>
 *   <li>{@link com.example.salon.controller.AuthController}: signing in and out, and passwords</li>
 *   <li>{@link com.example.salon.controller.UserController}: user accounts</li>
 *   <li>{@link com.example.salon.controller.AddressController},
 *       {@link com.example.salon.controller.ContactController}: addresses, phone numbers and emails</li>
 *   <li>{@link com.example.salon.controller.BusinessController}: salons</li>
 *   <li>{@link com.example.salon.controller.BusinessServiceController}: a salon's services and prices</li>
 *   <li>{@link com.example.salon.controller.BusinessHoursController}: a salon's opening hours</li>
 *   <li>{@link com.example.salon.controller.StaffController}: a salon's staff, and the services each
 *       performs</li>
 *   <li>{@link com.example.salon.controller.StaffScheduleController}: a staff member's working week and
 *       time off</li>
 *   <li>{@link com.example.salon.controller.CustomerController}: a salon's clients</li>
 *   <li>{@link com.example.salon.controller.AppointmentController}: appointments</li>
 *   <li>{@link com.example.salon.controller.AvailabilityController}: open times for booking a service</li>
 *   <li>{@link com.example.salon.controller.NotificationController}: what the signed-in user is told
 *       about</li>
 * </ul>
 */
package com.example.salon.controller;
