package com.example.salon.controller;

import com.example.salon.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** DB-14: booking, reading and changing appointments, on the salon's clock. */
class AppointmentControllerTest extends IntegrationTest
{
	private static final String GLOW = "/api/v1/businesses/" + Fixture.GLOW;
	private static final String APPOINTMENTS = GLOW + "/appointments";
	private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
	private static final DateTimeFormatter MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/** A time a week from today on Glow's clock, so it stays in the future whenever the tests run. */
	private static String nextWeek(int hour, int minute)
	{
		return LocalDate.now(BERLIN).plusWeeks(1).atTime(hour, minute).format(MINUTES);
	}

	private static String booking(long customerId, long staffId, long serviceId, String startsAt)
	{
		return """
				{"customer_id": %d, "staff_id": %d, "service_id": %d, "starts_at": "%s"}"""
				.formatted(customerId, staffId, serviceId, startsAt);
	}

	/** Olivia's haircut with Mia, the one service Mia performs. */
	private static String haircut(String startsAt)
	{
		return booking(Fixture.GLOW_CUSTOMER, Fixture.GLOW_STAFF, Fixture.GLOW_HAIRCUT, startsAt);
	}

	private ResultActions send(MockHttpServletRequestBuilder request, MockHttpSession session, String body)
			throws Exception
	{
		return mvc.perform(request.session(session).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	/** Returns the new appointment's Location. */
	private String book(MockHttpSession session, String body) throws Exception
	{
		return send(post(APPOINTMENTS), session, body)
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");
	}

	private static long idOf(String location)
	{
		return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
	}

	@Test
	void anEmployeeBooksAnAppointmentOnTheSalonsClock() throws Exception
	{
		MockHttpSession mia = loginAs(Fixture.GLOW_EMPLOYEE);
		String startsAt = nextWeek(10, 0);

		String location = send(post(APPOINTMENTS), mia, """
				{"customer_id": %d, "staff_id": %d, "service_id": %d, "starts_at": "%s", "notes": " Bring photos "}
				""".formatted(Fixture.GLOW_CUSTOMER, Fixture.GLOW_STAFF, Fixture.GLOW_HAIRCUT, startsAt))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(notNullValue()))
				.andExpect(jsonPath("$.customer.id").value(Fixture.GLOW_CUSTOMER))
				.andExpect(jsonPath("$.customer.first_name").value("Olivia"))
				.andExpect(jsonPath("$.customer.last_name").value("Client"))
				.andExpect(jsonPath("$.staff.id").value(Fixture.GLOW_STAFF))
				.andExpect(jsonPath("$.staff.first_name").value("Mia"))
				.andExpect(jsonPath("$.service.id").value(Fixture.GLOW_HAIRCUT))
				.andExpect(jsonPath("$.service.name").value("Signature Haircut"))
				.andExpect(jsonPath("$.starts_at").value(startsAt))
				// The haircut takes 45 minutes and costs 45.00.
				.andExpect(jsonPath("$.ends_at").value(nextWeek(10, 45)))
				.andExpect(content().string(containsString("\"price\":45.00")))
				.andExpect(jsonPath("$.status").value("BOOKED"))
				.andExpect(jsonPath("$.notes").value("Bring photos"))
				.andExpect(jsonPath("$.updated_at").value(nullValue()))
				.andReturn().getResponse().getHeader("Location");

		mvc.perform(get(location).session(mia))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.starts_at").value(startsAt));

		OffsetDateTime stored = jdbcTemplate.queryForObject(
				"SELECT starts_at FROM appointments WHERE id = ?", OffsetDateTime.class, idOf(location));
		assertThat(stored.toInstant()).isEqualTo(LocalDateTime.parse(startsAt).atZone(BERLIN).toInstant());
	}

	@Test
	void theServiceSetsLengthAndPriceAtBookingAndOnlyAChangeOfServiceResetsThem() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		String location = book(admin, haircut(nextWeek(10, 0)));

		send(put(GLOW + "/services/" + Fixture.GLOW_HAIRCUT), admin, """
				{"name": "Signature Haircut", "duration_minutes": 60, "price": 55.00, "is_active": true}
				""").andExpect(status().isOk());
		mvc.perform(get(location).session(admin))
				.andExpect(jsonPath("$.ends_at").value(nextWeek(10, 45)))
				.andExpect(content().string(containsString("\"price\":45.00")));

		// Moving it keeps what was agreed.
		send(put(location), admin, haircut(nextWeek(14, 0)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.starts_at").value(nextWeek(14, 0)))
				.andExpect(jsonPath("$.ends_at").value(nextWeek(14, 45)))
				.andExpect(content().string(containsString("\"price\":45.00")))
				.andExpect(jsonPath("$.updated_at").value(notNullValue()));

		// A different service brings its own.
		send(put(GLOW + "/staff/" + Fixture.GLOW_STAFF + "/services"), admin,
				"{\"service_ids\": [" + Fixture.GLOW_HAIRCUT + ", " + Fixture.GLOW_MANICURE + "]}")
				.andExpect(status().isOk());
		send(put(location), admin,
				booking(Fixture.GLOW_CUSTOMER, Fixture.GLOW_STAFF, Fixture.GLOW_MANICURE, nextWeek(14, 0)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.service.name").value("Classic Manicure"))
				.andExpect(jsonPath("$.ends_at").value(nextWeek(14, 30)))
				.andExpect(content().string(containsString("\"price\":25.00")));
	}

	@Test
	void aStaffMemberCannotBeBookedTwiceAtOnce() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		book(admin, haircut(nextWeek(10, 0)));

		send(post(APPOINTMENTS), admin, haircut(nextWeek(10, 30)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("Staff with id " + Fixture.GLOW_STAFF + " already has an appointment at that time."));

		// Straight after is fine, but it can't then be moved into the first one.
		String second = book(admin, haircut(nextWeek(10, 45)));
		send(put(second), admin, haircut(nextWeek(10, 15))).andExpect(status().isConflict());
		mvc.perform(get(second).session(admin)).andExpect(jsonPath("$.starts_at").value(nextWeek(10, 45)));
	}

	@Test
	void aBookingNeedsTheSalonsOwnCustomerAndAnActiveStaffMemberWhoPerformsAnActiveService() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		String at = nextWeek(10, 0);

		send(post(APPOINTMENTS), admin, booking(Fixture.URBAN_CUSTOMER, Fixture.GLOW_STAFF, Fixture.GLOW_HAIRCUT, at))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Customer with id 2 is not a customer of this business."));
		send(post(APPOINTMENTS), admin, booking(Fixture.GLOW_CUSTOMER, Fixture.URBAN_STAFF, Fixture.GLOW_HAIRCUT, at))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Staff with id 2 is not on the staff of this business."));
		send(post(APPOINTMENTS), admin, booking(Fixture.GLOW_CUSTOMER, Fixture.GLOW_STAFF, Fixture.URBAN_FADE, at))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Service with id 3 is not offered by this business."));
		send(post(APPOINTMENTS), admin, booking(Fixture.GLOW_CUSTOMER, Fixture.GLOW_STAFF, Fixture.GLOW_MANICURE, at))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Staff with id 1 does not perform service 2."));

		send(put(GLOW + "/services/" + Fixture.GLOW_HAIRCUT), admin, """
				{"name": "Signature Haircut", "duration_minutes": 45, "price": 45.00, "is_active": false}
				""").andExpect(status().isOk());
		send(post(APPOINTMENTS), admin, haircut(at))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Service with id 1 is not active."));

		send(put(GLOW + "/staff/" + Fixture.GLOW_STAFF), admin, """
				{"title": "Senior Stylist", "is_active": false, "hired_at": "2021-03-15"}
				""").andExpect(status().isOk());
		send(post(APPOINTMENTS), admin, haircut(at))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Staff with id 1 is not active."));

		mvc.perform(delete(GLOW + "/customers/" + Fixture.GLOW_CUSTOMER).session(admin)).andExpect(status().isOk());
		send(post(APPOINTMENTS), admin, haircut(at))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Customer with id 1 is not a customer of this business."));

		send(post(APPOINTMENTS), admin, "{}").andExpect(status().isBadRequest());
		send(post(APPOINTMENTS), admin, haircut("2027-03-02 10:00")).andExpect(status().isBadRequest());
	}

	@Test
	void theSalonsStaffCanSeeBookAndChangeItsAppointmentsButNoOneElseCan() throws Exception
	{
		String location = book(loginAs(Fixture.GLOW_ADMIN), haircut(nextWeek(10, 0)));

		MockHttpSession mia = loginAs(Fixture.GLOW_EMPLOYEE);
		mvc.perform(get(APPOINTMENTS).session(mia)).andExpect(jsonPath("$", hasSize(1)));
		send(put(location), mia, haircut(nextWeek(11, 0))).andExpect(status().isOk());
		mvc.perform(get(APPOINTMENTS).session(loginAs(Fixture.SUPER_ADMIN))).andExpect(jsonPath("$", hasSize(1)));

		for (String outsider : new String[] {Fixture.URBAN_ADMIN, Fixture.URBAN_EMPLOYEE}) {
			MockHttpSession session = loginAs(outsider);
			mvc.perform(get(APPOINTMENTS).session(session)).andExpect(status().isForbidden());
			mvc.perform(get(location).session(session)).andExpect(status().isForbidden());
			send(post(APPOINTMENTS), session, haircut(nextWeek(12, 0))).andExpect(status().isForbidden());
			send(put(location), session, haircut(nextWeek(12, 0))).andExpect(status().isForbidden());
		}
		mvc.perform(get(APPOINTMENTS)).andExpect(status().isUnauthorized());
	}

	@Test
	void anotherSalonsAppointmentIsNotFoundThroughYourPath() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);
		String urban = "/api/v1/businesses/" + Fixture.URBAN;
		send(put(urban + "/staff/" + Fixture.URBAN_STAFF + "/services"), superAdmin,
				"{\"service_ids\": [" + Fixture.URBAN_FADE + "]}")
				.andExpect(status().isOk());
		String created = send(post(urban + "/appointments"), superAdmin,
				booking(Fixture.URBAN_CUSTOMER, Fixture.URBAN_STAFF, Fixture.URBAN_FADE, nextWeek(10, 0)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		long urbanAppointment = ((Number) JsonPath.read(created, "$.id")).longValue();

		mvc.perform(get(APPOINTMENTS + "/" + urbanAppointment).session(superAdmin)).andExpect(status().isNotFound());
		send(put(APPOINTMENTS + "/" + urbanAppointment), superAdmin, haircut(nextWeek(10, 0)))
				.andExpect(status().isNotFound());
		mvc.perform(get(APPOINTMENTS).session(superAdmin)).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void theListIsInOrderAndCanBeNarrowedByDaysStaffMemberAndCustomer() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		LocalDate day = LocalDate.now(BERLIN).plusWeeks(1);
		LocalDate nextDay = day.plusDays(1);

		// Anna takes haircuts too, and Ella is a second customer.
		String anna = send(post(GLOW + "/staff"), admin, """
				{"user_id": %d, "title": "Owner", "is_active": true, "hired_at": "2019-05-01"}
				""".formatted(Fixture.GLOW_ADMIN_ID)).andReturn().getResponse().getHeader("Location");
		long annaId = idOf(anna);
		send(put(anna + "/services"), admin, "{\"service_ids\": [" + Fixture.GLOW_HAIRCUT + "]}")
				.andExpect(status().isOk());
		long ella = idOf(send(post(GLOW + "/customers"), admin, """
				{"first_name": "Ella", "last_name": "Walk-in"}
				""").andReturn().getResponse().getHeader("Location"));

		long oliviaWithMia = idOf(book(admin, haircut(day.atTime(14, 0).format(MINUTES))));
		long ellaWithAnna = idOf(book(admin, booking(ella, annaId, Fixture.GLOW_HAIRCUT, day.atTime(10, 0).format(MINUTES))));
		long oliviaNextDay = idOf(book(admin, haircut(nextDay.atTime(10, 0).format(MINUTES))));

		mvc.perform(get(APPOINTMENTS).session(admin))
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[0].id").value(ellaWithAnna))
				.andExpect(jsonPath("$[1].id").value(oliviaWithMia))
				.andExpect(jsonPath("$[2].id").value(oliviaNextDay));
		mvc.perform(get(APPOINTMENTS).session(admin).param("from", day.toString()).param("to", day.toString()))
				.andExpect(jsonPath("$", hasSize(2)));
		mvc.perform(get(APPOINTMENTS).session(admin).param("from", nextDay.toString()))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(oliviaNextDay));
		mvc.perform(get(APPOINTMENTS).session(admin).param("staff_id", String.valueOf(annaId)))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].staff.first_name").value("Anna"));
		mvc.perform(get(APPOINTMENTS).session(admin).param("customer_id", String.valueOf(Fixture.GLOW_CUSTOMER))
						.param("to", day.toString()))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(oliviaWithMia));

		mvc.perform(get(APPOINTMENTS).session(admin).param("from", nextDay.toString()).param("to", day.toString()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("to must not be before from."));
		mvc.perform(get(APPOINTMENTS).session(admin).param("from", "next week"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("from has an invalid value"));
		mvc.perform(get(APPOINTMENTS).session(admin).param("staff_id", "Mia")).andExpect(status().isBadRequest());
	}

	@Test
	void aStaffMemberServiceOrCustomerWithUpcomingAppointmentsCannotBeRemoved() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		book(admin, haircut(nextWeek(10, 0)));

		mvc.perform(delete(GLOW + "/staff/" + Fixture.GLOW_STAFF).session(admin))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("Staff with id 1 still has upcoming appointments. Cancel or move them first."));
		mvc.perform(delete(GLOW + "/services/" + Fixture.GLOW_HAIRCUT).session(admin))
				.andExpect(status().isConflict());
		mvc.perform(delete(GLOW + "/customers/" + Fixture.GLOW_CUSTOMER).session(admin))
				.andExpect(status().isConflict());
	}

	@Test
	void aPastAppointmentStillNamesItsCustomerStaffMemberAndServiceOnceTheyAreRemoved() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		String location = book(admin, haircut("2025-03-04T10:00"));

		mvc.perform(delete(GLOW + "/customers/" + Fixture.GLOW_CUSTOMER).session(admin)).andExpect(status().isOk());
		mvc.perform(delete(GLOW + "/services/" + Fixture.GLOW_HAIRCUT).session(admin)).andExpect(status().isOk());
		mvc.perform(delete(GLOW + "/staff/" + Fixture.GLOW_STAFF).session(admin)).andExpect(status().isOk());

		mvc.perform(get(location).session(admin))
				.andExpect(jsonPath("$.customer.first_name").value("Olivia"))
				.andExpect(jsonPath("$.staff.first_name").value("Mia"))
				.andExpect(jsonPath("$.service.name").value("Signature Haircut"));
		send(post(APPOINTMENTS), admin, haircut(nextWeek(10, 0))).andExpect(status().isBadRequest());
	}

	@Test
	void aStaffMembersAccountCannotBeDeletedWhileAppointmentsPointAtThem() throws Exception
	{
		book(loginAs(Fixture.GLOW_ADMIN), haircut("2025-03-04T10:00"));
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);

		mvc.perform(delete("/api/v1/users/" + Fixture.GLOW_EMPLOYEE_ID).session(superAdmin))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"User with id 4 is the staff member on appointments, so the account can't be deleted."));

		// Nothing of the attempt is left behind: the account and its contact are still there.
		mvc.perform(get("/api/v1/users/" + Fixture.GLOW_EMPLOYEE_ID).session(superAdmin)).andExpect(status().isOk());
		mvc.perform(get("/api/v1/contacts/" + Fixture.GLOW_EMPLOYEE_PERSONAL_CONTACT).session(superAdmin))
				.andExpect(status().isOk());
	}
}
