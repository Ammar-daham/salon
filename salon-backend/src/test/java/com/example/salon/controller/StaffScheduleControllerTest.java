package com.example.salon.controller;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** DB-14: a staff member's weekly hours, on the salon's clock. */
class StaffScheduleControllerTest extends IntegrationTest
{
	private static final String MIA = "/api/v1/businesses/" + Fixture.GLOW + "/staff/" + Fixture.GLOW_STAFF;

	private ResultActions send(MockHttpServletRequestBuilder request, MockHttpSession session, String body)
			throws Exception
	{
		return mvc.perform(request.session(session).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private static String shift(String day, String startsAt, String endsAt)
	{
		return """
				{"day_of_week": "%s", "starts_at": "%s", "ends_at": "%s"}""".formatted(day, startsAt, endsAt);
	}

	private static String week(String... shifts)
	{
		return "{\"hours\": [" + String.join(", ", shifts) + "]}";
	}

	@Test
	void theSalonsStaffCanSeeAColleaguesWeekButNoOneElseCan() throws Exception
	{
		mvc.perform(get(MIA + "/schedule").session(loginAs(Fixture.GLOW_EMPLOYEE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.timezone").value("Europe/Berlin"))
				.andExpect(jsonPath("$.hours", hasSize(2)))
				.andExpect(jsonPath("$.hours[0].day_of_week").value("TUESDAY"))
				.andExpect(jsonPath("$.hours[0].starts_at").value("09:00"))
				.andExpect(jsonPath("$.hours[0].ends_at").value("17:00"));
		mvc.perform(get(MIA + "/schedule").session(loginAs(Fixture.GLOW_ADMIN))).andExpect(status().isOk());

		mvc.perform(get(MIA + "/schedule").session(loginAs(Fixture.URBAN_EMPLOYEE))).andExpect(status().isForbidden());
		mvc.perform(get(MIA + "/schedule").session(loginAs(Fixture.URBAN_ADMIN))).andExpect(status().isForbidden());
		mvc.perform(get(MIA + "/schedule")).andExpect(status().isUnauthorized());
	}

	@Test
	void adminReplacesTheWholeWeekAndGetsItBackInOrder() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		send(put(MIA + "/schedule"), admin, week(shift("FRIDAY", "12:00", "20:00"),
				shift("MONDAY", "14:00", "18:00"), shift("MONDAY", "08:00", "12:00")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hours", hasSize(3)))
				.andExpect(jsonPath("$.hours[0].starts_at").value("08:00"))
				.andExpect(jsonPath("$.hours[1].starts_at").value("14:00"))
				.andExpect(jsonPath("$.hours[2].day_of_week").value("FRIDAY"));

		// Tuesday and Wednesday weren't listed, so they are days off now.
		mvc.perform(get(MIA + "/schedule").session(admin)).andExpect(jsonPath("$.hours", hasSize(3)));

		send(put(MIA + "/schedule"), admin, week()).andExpect(status().isOk());
		mvc.perform(get(MIA + "/schedule").session(admin)).andExpect(jsonPath("$.hours", hasSize(0)));
	}

	@Test
	void aShiftMustEndAfterItStartsAndADaysShiftsMustNotOverlap() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		send(put(MIA + "/schedule"), admin, week(shift("MONDAY", "17:00", "09:00")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("MONDAY 17:00-09:00: ends_at must be after starts_at."));
		send(put(MIA + "/schedule"), admin, week(shift("MONDAY", "09:00", "13:00"), shift("MONDAY", "12:30", "17:00")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("MONDAY 09:00-13:00 overlaps 12:30-17:00."));
		send(put(MIA + "/schedule"), admin, week(shift("MONDAY", "9am", "17:00"))).andExpect(status().isBadRequest());
		send(put(MIA + "/schedule"), admin, "{}").andExpect(status().isBadRequest());

		mvc.perform(get(MIA + "/schedule").session(admin)).andExpect(jsonPath("$.hours", hasSize(2)));
	}

	@Test
	void onlyAnAdminOfTheSalonCanChangeAWeekAndOnlyForItsOwnStaff() throws Exception
	{
		String body = week(shift("MONDAY", "09:00", "17:00"));

		send(put(MIA + "/schedule"), loginAs(Fixture.GLOW_EMPLOYEE), body).andExpect(status().isForbidden());
		send(put(MIA + "/schedule"), loginAs(Fixture.URBAN_ADMIN), body).andExpect(status().isForbidden());
		// Urban's staff member, reached through Glow's path.
		send(put("/api/v1/businesses/" + Fixture.GLOW + "/staff/" + Fixture.URBAN_STAFF + "/schedule"),
				loginAs(Fixture.GLOW_ADMIN), body)
				.andExpect(status().isNotFound());
	}
}
