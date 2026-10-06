package com.example.salon.controller;

import com.example.salon.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DB-14: open slots for a service, per staff member. seed.sql opens Glow Tuesday to Friday 09:00-18:00, and Mia,
 * its one stylist, does 45-minute haircuts on Tuesdays and Wednesdays 09:00-17:00.
 */
class AvailabilityControllerTest extends IntegrationTest
{
	private static final String GLOW = "/api/v1/businesses/" + Fixture.GLOW;
	private static final String MIA = GLOW + "/staff/" + Fixture.GLOW_STAFF;
	private static final String HAIRCUTS = GLOW + "/services/" + Fixture.GLOW_HAIRCUT + "/availability";
	private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

	/** The first Tuesday after today on Glow's clock, so none of it has passed whenever the tests run. */
	private static LocalDate nextTuesday()
	{
		return LocalDate.now(BERLIN).with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
	}

	private static String at(LocalDate day, String time)
	{
		return day + "T" + time;
	}

	private ResultActions send(MockHttpServletRequestBuilder request, MockHttpSession session, String body)
			throws Exception
	{
		return mvc.perform(request.session(session).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	/** Mia's slot starts that day, from a successful request. */
	private List<String> miasStarts(MockHttpSession session, LocalDate from, LocalDate to) throws Exception
	{
		String json = mvc.perform(get(HAIRCUTS).session(session)
						.param("from", from.toString())
						.param("to", to.toString())
						.param("staff_id", String.valueOf(Fixture.GLOW_STAFF)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(json, "$.staff[0].slots[*].starts_at");
	}

	private String bookMia(MockHttpSession session, String startsAt) throws Exception
	{
		return send(post(GLOW + "/appointments"), session, """
				{"customer_id": %d, "staff_id": %d, "service_id": %d, "starts_at": "%s"}
				""".formatted(Fixture.GLOW_CUSTOMER, Fixture.GLOW_STAFF, Fixture.GLOW_HAIRCUT, startsAt))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");
	}

	@Test
	void aStaffMembersSlotsFillTheirShiftOnTheQuarterHours() throws Exception
	{
		LocalDate tuesday = nextTuesday();
		MockHttpSession mia = loginAs(Fixture.GLOW_EMPLOYEE);

		mvc.perform(get(HAIRCUTS).session(mia).param("from", tuesday.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.timezone").value("Europe/Berlin"))
				.andExpect(jsonPath("$.duration_minutes").value(45))
				.andExpect(jsonPath("$.staff", hasSize(1)))
				.andExpect(jsonPath("$.staff[0].id").value(Fixture.GLOW_STAFF))
				.andExpect(jsonPath("$.staff[0].first_name").value("Mia"))
				.andExpect(jsonPath("$.staff[0].last_name").value("Stylist"))
				.andExpect(jsonPath("$.staff[0].slots[0].starts_at").value(at(tuesday, "09:00")))
				.andExpect(jsonPath("$.staff[0].slots[0].ends_at").value(at(tuesday, "09:45")));

		// Her shift ends at 17:00, an hour before Glow closes, so the last haircut starts at 16:15.
		assertThat(miasStarts(mia, tuesday, tuesday))
				.hasSize(30)
				.startsWith(at(tuesday, "09:00"), at(tuesday, "09:15"))
				.endsWith(at(tuesday, "16:15"));
	}

	@Test
	void slotsNeedTheSalonOpenAndTheStaffMemberOnShift() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		LocalDate tuesday = nextTuesday();
		// Glow is closed on Mondays, and closes at 18:00 on Tuesdays.
		send(put(MIA + "/schedule"), admin, """
				{"hours": [{"day_of_week": "MONDAY", "starts_at": "09:00", "ends_at": "17:00"},
				           {"day_of_week": "TUESDAY", "starts_at": "10:10", "ends_at": "20:00"}]}
				""").andExpect(status().isOk());

		// A shift starting at 10:10 is offered from the next quarter hour.
		assertThat(miasStarts(admin, tuesday, tuesday.plusDays(6)))
				.hasSize(29)
				.startsWith(at(tuesday, "10:15"))
				.endsWith(at(tuesday, "17:15"))
				.allMatch(start -> start.startsWith(tuesday.toString()));
	}

	@Test
	void aLunchBreakSplitsTheDayButIntervalsThatTouchDoNot() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		LocalDate tuesday = nextTuesday();
		String tuesdayHours = """
				{"hours": [{"day_of_week": "TUESDAY", "opens_at": "09:00", "closes_at": "12:00"},
				           {"day_of_week": "TUESDAY", "opens_at": "%s", "closes_at": "18:00"}]}
				""";

		send(put(GLOW + "/hours"), admin, tuesdayHours.formatted("13:00")).andExpect(status().isOk());
		assertThat(miasStarts(admin, tuesday, tuesday))
				.contains(at(tuesday, "11:15"), at(tuesday, "13:00"))
				.doesNotContain(at(tuesday, "11:30"), at(tuesday, "12:45"));

		// Open 09:00-12:00 and 12:00-18:00 is open all day: a haircut can run across 12:00.
		send(put(GLOW + "/hours"), admin, tuesdayHours.formatted("12:00")).andExpect(status().isOk());
		assertThat(miasStarts(admin, tuesday, tuesday)).hasSize(30).contains(at(tuesday, "11:30"));
	}

	@Test
	void appointmentsAndTimeOffTakeTheirTimeUntilTheAppointmentIsCancelled() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		LocalDate tuesday = nextTuesday();
		String appointment = bookMia(admin, at(tuesday, "10:00"));
		send(post(MIA + "/time-off"), admin, """
				{"starts_at": "%s", "ends_at": "%s"}
				""".formatted(at(tuesday, "13:00"), at(tuesday, "14:00"))).andExpect(status().isCreated());

		// A haircut can end as the 10:00 one starts or begin as it ends, but not overlap it; same for the time off.
		assertThat(miasStarts(admin, tuesday, tuesday))
				.hasSize(30 - 5 - 6)
				.contains(at(tuesday, "09:15"), at(tuesday, "10:45"), at(tuesday, "12:15"), at(tuesday, "14:00"))
				.doesNotContain(at(tuesday, "09:30"), at(tuesday, "10:30"), at(tuesday, "12:30"), at(tuesday, "13:45"));

		send(put(appointment + "/status"), admin, "{\"status\": \"CANCELLED\"}").andExpect(status().isOk());

		assertThat(miasStarts(admin, tuesday, tuesday)).hasSize(30 - 6).contains(at(tuesday, "10:00"));
	}

	@Test
	void openingHoursAreOnTheSalonsClockAndAppointmentsAreFixedInstants() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		LocalDate tuesday = nextTuesday();
		bookMia(admin, at(tuesday, "10:00"));

		send(put(GLOW), admin, """
				{"name": "Glow Beauty Studio", "timezone": "Europe/London"}
				""").andExpect(status().isOk());

		// Still open from 09:00, now in London, where the haircut booked for 10:00 in Berlin is at 09:00.
		assertThat(miasStarts(admin, tuesday, tuesday))
				.startsWith(at(tuesday, "09:45"))
				.endsWith(at(tuesday, "16:15"));
	}

	@Test
	void onlyActiveStaffWhoPerformTheServiceAreOffered() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		LocalDate tuesday = nextTuesday();
		// Anna does haircuts too, on Tuesday mornings.
		String anna = send(post(GLOW + "/staff"), admin, """
				{"user_id": %d, "title": "Owner", "is_active": true, "hired_at": "2019-05-01"}
				""".formatted(Fixture.GLOW_ADMIN_ID))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");
		long annaId = Long.parseLong(anna.substring(anna.lastIndexOf('/') + 1));
		send(put(anna + "/services"), admin, "{\"service_ids\": [" + Fixture.GLOW_HAIRCUT + "]}")
				.andExpect(status().isOk());
		send(put(anna + "/schedule"), admin, """
				{"hours": [{"day_of_week": "TUESDAY", "starts_at": "09:00", "ends_at": "12:00"}]}
				""").andExpect(status().isOk());

		mvc.perform(get(HAIRCUTS).session(admin).param("from", tuesday.toString()))
				.andExpect(jsonPath("$.staff", hasSize(2)))
				.andExpect(jsonPath("$.staff[0].first_name").value("Anna"))
				.andExpect(jsonPath("$.staff[0].slots", hasSize(10)))
				.andExpect(jsonPath("$.staff[1].first_name").value("Mia"));
		mvc.perform(get(HAIRCUTS).session(admin).param("staff_id", String.valueOf(annaId)))
				.andExpect(jsonPath("$.staff", hasSize(1)))
				.andExpect(jsonPath("$.staff[0].id").value(annaId));

		// Nobody does manicures.
		mvc.perform(get(GLOW + "/services/" + Fixture.GLOW_MANICURE + "/availability").session(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.staff", hasSize(0)));

		send(put(MIA), admin, """
				{"title": "Senior Stylist", "is_active": false, "hired_at": "2021-03-15"}
				""").andExpect(status().isOk());
		mvc.perform(get(HAIRCUTS).session(admin))
				.andExpect(jsonPath("$.staff", hasSize(1)))
				.andExpect(jsonPath("$.staff[0].first_name").value("Anna"));
		mvc.perform(get(HAIRCUTS).session(admin).param("staff_id", String.valueOf(Fixture.GLOW_STAFF)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Staff with id 1 is not active."));
		mvc.perform(get(HAIRCUTS).session(admin).param("staff_id", String.valueOf(Fixture.URBAN_STAFF)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Staff with id 2 does not perform service 1."));
	}

	@Test
	void theServiceMustBeOneOfTheSalonsCurrentActiveServices() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		mvc.perform(get(GLOW + "/services/" + Fixture.URBAN_FADE + "/availability").session(admin))
				.andExpect(status().isNotFound());

		mvc.perform(delete(GLOW + "/services/" + Fixture.GLOW_MANICURE).session(admin)).andExpect(status().isOk());
		mvc.perform(get(GLOW + "/services/" + Fixture.GLOW_MANICURE + "/availability").session(admin))
				.andExpect(status().isNotFound());

		send(put(GLOW + "/services/" + Fixture.GLOW_HAIRCUT), admin, """
				{"name": "Signature Haircut", "duration_minutes": 45, "price": 45.00, "is_active": false}
				""").andExpect(status().isOk());
		mvc.perform(get(HAIRCUTS).session(admin))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Service with id 1 is not active."));
	}

	@Test
	void aRequestCoversUpToThirtyOneDaysAndDaysAlreadyOverHaveNoSlots() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		LocalDate tuesday = nextTuesday();

		// Today, unless asked otherwise.
		mvc.perform(get(HAIRCUTS).session(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.staff", hasSize(1)));
		assertThat(miasStarts(admin, tuesday.minusWeeks(2), tuesday.minusWeeks(2))).isEmpty();
		// Five Tuesdays and five Wednesdays.
		assertThat(miasStarts(admin, tuesday, tuesday.plusDays(30))).hasSize(10 * 30);

		mvc.perform(get(HAIRCUTS).session(admin)
						.param("from", tuesday.toString()).param("to", tuesday.plusDays(31).toString()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ask for at most 31 days at a time."));
		mvc.perform(get(HAIRCUTS).session(admin)
						.param("from", tuesday.toString()).param("to", tuesday.minusDays(1).toString()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("to must not be before from."));
		mvc.perform(get(HAIRCUTS).session(admin).param("from", "tuesday")).andExpect(status().isBadRequest());
	}

	@Test
	void onlyTheSalonsStaffCanSeeItsAvailability() throws Exception
	{
		mvc.perform(get(HAIRCUTS).session(loginAs(Fixture.GLOW_EMPLOYEE))).andExpect(status().isOk());
		mvc.perform(get(HAIRCUTS).session(loginAs(Fixture.SUPER_ADMIN))).andExpect(status().isOk());

		mvc.perform(get(HAIRCUTS).session(loginAs(Fixture.URBAN_ADMIN))).andExpect(status().isForbidden());
		mvc.perform(get(HAIRCUTS).session(loginAs(Fixture.URBAN_EMPLOYEE))).andExpect(status().isForbidden());
		mvc.perform(get(HAIRCUTS)).andExpect(status().isUnauthorized());
	}
}
