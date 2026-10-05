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

import java.time.Instant;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** DB-14: a staff member's weekly hours and time off, on the salon's clock. */
class StaffScheduleControllerTest extends IntegrationTest
{
	private static final String MIA = "/api/v1/businesses/" + Fixture.GLOW + "/staff/" + Fixture.GLOW_STAFF;

	@Autowired
	private JdbcTemplate jdbcTemplate;

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

	private static String timeOff(String startsAt, String endsAt)
	{
		return """
				{"starts_at": "%s", "ends_at": "%s", "note": "Training"}""".formatted(startsAt, endsAt);
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

	@Test
	void timeOffIsWrittenOnTheSalonsClockAndStoredAsAnInstant() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		String location = send(post(MIA + "/time-off"), admin, timeOff("2026-07-01T09:00", "2026-07-01T12:00"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(notNullValue()))
				.andExpect(jsonPath("$.starts_at").value("2026-07-01T09:00"))
				.andExpect(jsonPath("$.ends_at").value("2026-07-01T12:00"))
				.andExpect(jsonPath("$.note").value("Training"))
				.andExpect(jsonPath("$.updated_at").value(nullValue()))
				.andReturn().getResponse().getHeader("Location");

		// 09:00 in Berlin in July is 07:00 UTC.
		long id = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
		OffsetDateTime stored = jdbcTemplate.queryForObject(
				"SELECT starts_at FROM staff_time_off WHERE id = ?", OffsetDateTime.class, id);
		assertThat(stored.toInstant()).isEqualTo(Instant.parse("2026-07-01T07:00:00Z"));

		// Moving the salon to London keeps the instant, so the same absence reads an hour earlier.
		send(put("/api/v1/businesses/" + Fixture.GLOW), admin, """
				{"name": "Glow Beauty Studio", "timezone": "Europe/London"}
				""").andExpect(status().isOk());
		mvc.perform(get(location).session(admin)).andExpect(jsonPath("$.starts_at").value("2026-07-01T08:00"));
	}

	@Test
	void adminCanMoveAndCancelTimeOff() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		String christmas = MIA + "/time-off/" + Fixture.GLOW_STAFF_CHRISTMAS;

		mvc.perform(get(MIA + "/time-off").session(admin))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].starts_at").value("2026-12-24T00:00"))
				.andExpect(jsonPath("$[0].note").value("Christmas"));

		send(put(christmas), admin, """
				{"starts_at": "2026-12-23T00:00", "ends_at": "2026-12-28T00:00"}
				""").andExpect(status().isOk());
		mvc.perform(get(christmas).session(admin))
				.andExpect(jsonPath("$.starts_at").value("2026-12-23T00:00"))
				.andExpect(jsonPath("$.note").value(nullValue()))
				.andExpect(jsonPath("$.updated_at").value(notNullValue()));

		mvc.perform(delete(christmas).session(admin)).andExpect(status().isOk());
		mvc.perform(get(christmas).session(admin)).andExpect(status().isNotFound());
	}

	@Test
	void overlappingTimeOffIsAConflictAndBackwardsTimeOffIsRejected() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		send(post(MIA + "/time-off"), admin, timeOff("2026-12-26T12:00", "2026-12-30T00:00"))
				.andExpect(status().isConflict());
		send(post(MIA + "/time-off"), admin, timeOff("2026-12-27T00:00", "2026-12-26T00:00"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("ends_at must be after starts_at."));
		send(post(MIA + "/time-off"), admin, timeOff("2026-12-28", "2026-12-29")).andExpect(status().isBadRequest());

		// Starting the moment Christmas ends is fine.
		send(post(MIA + "/time-off"), admin, timeOff("2026-12-27T00:00", "2026-12-28T00:00"))
				.andExpect(status().isCreated());
	}

	@Test
	void timeOffIsSeenOnlyByAnAdminAndTheStaffMemberThemselves() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		String anna = send(post("/api/v1/businesses/" + Fixture.GLOW + "/staff"), admin, """
				{"user_id": %d, "title": "Owner", "is_active": true, "hired_at": "2019-05-01"}
				""".formatted(Fixture.GLOW_ADMIN_ID))
				.andReturn().getResponse().getHeader("Location");

		MockHttpSession mia = loginAs(Fixture.GLOW_EMPLOYEE);
		mvc.perform(get(MIA + "/time-off").session(mia)).andExpect(status().isOk());
		mvc.perform(get(anna + "/time-off").session(mia)).andExpect(status().isForbidden());
		mvc.perform(get(anna + "/time-off").session(admin)).andExpect(status().isOk());

		// Seeing it isn't booking it.
		send(post(MIA + "/time-off"), mia, timeOff("2027-01-04T09:00", "2027-01-04T12:00")).andExpect(status().isForbidden());
		mvc.perform(get(MIA + "/time-off").session(loginAs(Fixture.URBAN_ADMIN))).andExpect(status().isForbidden());
	}

	@Test
	void anotherStaffMembersTimeOffIsNotFoundThroughYourPath() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);
		String urbanStaff = "/api/v1/businesses/" + Fixture.URBAN + "/staff/" + Fixture.URBAN_STAFF;
		String created = send(post(urbanStaff + "/time-off"), superAdmin, timeOff("2027-02-01T09:00", "2027-02-01T17:00"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		long urbanTimeOff = ((Number) JsonPath.read(created, "$.id")).longValue();

		mvc.perform(get(MIA + "/time-off/" + urbanTimeOff).session(superAdmin)).andExpect(status().isNotFound());
		mvc.perform(delete(MIA + "/time-off/" + urbanTimeOff).session(superAdmin)).andExpect(status().isNotFound());
		mvc.perform(get(urbanStaff + "/time-off/" + urbanTimeOff).session(superAdmin)).andExpect(status().isOk());
	}
}
