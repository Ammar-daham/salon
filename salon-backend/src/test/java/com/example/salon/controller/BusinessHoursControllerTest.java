package com.example.salon.controller;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** DB-14: a salon's weekly opening hours, on its own clock (DB-09). */
class BusinessHoursControllerTest extends IntegrationTest
{
	private static final String SPLIT_MONDAY_AND_SATURDAY = """
			{"hours": [
			  {"day_of_week": "SATURDAY", "opens_at": "10:00", "closes_at": "14:00"},
			  {"day_of_week": "MONDAY", "opens_at": "13:00", "closes_at": "18:30"},
			  {"day_of_week": "MONDAY", "opens_at": "09:00", "closes_at": "12:00"}
			]}
			""";

	private static String hours(long businessId)
	{
		return "/api/v1/businesses/" + businessId + "/hours";
	}

	private ResultActions replace(long businessId, MockHttpSession session, String body) throws Exception
	{
		return mvc.perform(put(hours(businessId)).session(session).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private static String week(String... intervals)
	{
		return "{\"hours\": [" + String.join(", ", intervals) + "]}";
	}

	private static String interval(String day, String opensAt, String closesAt)
	{
		return """
				{"day_of_week": "%s", "opens_at": "%s", "closes_at": "%s"}""".formatted(day, opensAt, closesAt);
	}

	@Test
	void anyoneSignedInCanReadAnApprovedSalonsHoursWithItsTimeZone() throws Exception
	{
		mvc.perform(get(hours(Fixture.GLOW)).session(loginAs(Fixture.URBAN_EMPLOYEE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.timezone").value("Europe/Berlin"))
				.andExpect(jsonPath("$.hours", hasSize(5)))
				.andExpect(jsonPath("$.hours[0].day_of_week").value("TUESDAY"))
				.andExpect(jsonPath("$.hours[0].opens_at").value("09:00"))
				.andExpect(jsonPath("$.hours[0].closes_at").value("18:00"))
				.andExpect(jsonPath("$.hours[4].day_of_week").value("SATURDAY"));

		mvc.perform(get(hours(Fixture.URBAN)).session(loginAs(Fixture.GLOW_ADMIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hours", hasSize(0)));
	}

	@Test
	void adminReplacesTheWholeWeekAndGetsItBackInOrder() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		replace(Fixture.GLOW, admin, SPLIT_MONDAY_AND_SATURDAY)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.timezone").value("Europe/Berlin"))
				.andExpect(jsonPath("$.hours", hasSize(3)))
				.andExpect(jsonPath("$.hours[0].opens_at").value("09:00"))
				.andExpect(jsonPath("$.hours[1].opens_at").value("13:00"))
				.andExpect(jsonPath("$.hours[1].closes_at").value("18:30"))
				.andExpect(jsonPath("$.hours[2].day_of_week").value("SATURDAY"));

		// Tuesday to Friday weren't listed, so they are closed now.
		mvc.perform(get(hours(Fixture.GLOW)).session(admin))
				.andExpect(jsonPath("$.hours", hasSize(3)))
				.andExpect(jsonPath("$.hours[0].day_of_week").value("MONDAY"))
				.andExpect(jsonPath("$.hours[1].day_of_week").value("MONDAY"));

		replace(Fixture.GLOW, admin, week()).andExpect(status().isOk());
		mvc.perform(get(hours(Fixture.GLOW)).session(admin)).andExpect(jsonPath("$.hours", hasSize(0)));
	}

	@Test
	void onlyAnAdminOfTheSalonOrASuperAdminCanChangeItsHours() throws Exception
	{
		replace(Fixture.GLOW, loginAs(Fixture.GLOW_EMPLOYEE), SPLIT_MONDAY_AND_SATURDAY).andExpect(status().isForbidden());
		replace(Fixture.GLOW, loginAs(Fixture.URBAN_ADMIN), SPLIT_MONDAY_AND_SATURDAY).andExpect(status().isForbidden());
		mvc.perform(get(hours(Fixture.GLOW)).session(loginAs(Fixture.GLOW_ADMIN))).andExpect(jsonPath("$.hours", hasSize(5)));

		replace(Fixture.URBAN, loginAs(Fixture.SUPER_ADMIN), SPLIT_MONDAY_AND_SATURDAY).andExpect(status().isOk());
	}

	@Test
	void anIntervalMustEndAfterItStartsAndADaysIntervalsMustNotOverlap() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		replace(Fixture.GLOW, admin, week(interval("MONDAY", "18:00", "09:00")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("MONDAY 18:00-09:00: closes_at must be after opens_at."));
		replace(Fixture.GLOW, admin, week(interval("MONDAY", "09:00", "09:00")))
				.andExpect(status().isBadRequest());
		replace(Fixture.GLOW, admin, week(interval("MONDAY", "13:00", "18:00"), interval("MONDAY", "09:00", "13:30")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("MONDAY 09:00-13:30 overlaps 13:00-18:00."));

		// Touching is fine, and the same times on different days don't clash.
		replace(Fixture.GLOW, admin, week(interval("MONDAY", "09:00", "13:00"), interval("MONDAY", "13:00", "18:00"),
				interval("TUESDAY", "09:00", "13:00")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hours", hasSize(3)));
	}

	@Test
	void malformedHoursAreRejectedAndNothingIsReplaced() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		replace(Fixture.GLOW, admin, "{}")
				.andExpect(status().isBadRequest());
		replace(Fixture.GLOW, admin, week("{\"day_of_week\": \"MONDAY\", \"opens_at\": \"09:00\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("closesAt must not be null")));
		replace(Fixture.GLOW, admin, week(interval("MONDAY", "9am", "17:00"))).andExpect(status().isBadRequest());
		replace(Fixture.GLOW, admin, week(interval("MONDAY", "09:00:30", "17:00"))).andExpect(status().isBadRequest());
		replace(Fixture.GLOW, admin, week(interval("FUNDAY", "09:00", "17:00"))).andExpect(status().isBadRequest());

		mvc.perform(get(hours(Fixture.GLOW)).session(admin)).andExpect(jsonPath("$.hours", hasSize(5)));
	}

	@Test
	void aSalonYouCannotSeeHasNoHoursYouCanSee() throws Exception
	{
		// Same rule as GET /businesses/{id}: a pending salon is a 404 to non-admins outside it.
		mvc.perform(get(hours(Fixture.SERENITY_PENDING)).session(loginAs(Fixture.GLOW_EMPLOYEE)))
				.andExpect(status().isNotFound());
		mvc.perform(get(hours(Fixture.SERENITY_PENDING)).session(loginAs(Fixture.SUPER_ADMIN)))
				.andExpect(status().isOk());
	}

	@Test
	void superAdminGetsNotFoundForASalonThatDoesNotExist() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);

		mvc.perform(get(hours(999)).session(superAdmin)).andExpect(status().isNotFound());
		replace(999, superAdmin, SPLIT_MONDAY_AND_SATURDAY).andExpect(status().isNotFound());
	}

	@Test
	void anonymousRequestsAreRejected() throws Exception
	{
		mvc.perform(get(hours(Fixture.GLOW))).andExpect(status().isUnauthorized());
		mvc.perform(put(hours(Fixture.GLOW)).contentType(MediaType.APPLICATION_JSON).content(SPLIT_MONDAY_AND_SATURDAY))
				.andExpect(status().isUnauthorized());
	}
}
