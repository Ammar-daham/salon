package com.example.salon.controller;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.util.Arrays;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BE-08 / BE-35 / DB-04: staff is now a real, reachable employment record instead of a stubbed
 * DAO with a constructor that dropped every field.
 */
class StaffControllerTest extends IntegrationTest
{
	@Test
	void adminCanHireAnExistingEmployeeIntoTheirOwnBusiness() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		String location = mvc.perform(post("/api/v1/businesses/" + Fixture.GLOW + "/staff").session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"user_id": %d, "title": "Colorist", "is_active": true, "hired_at": "2024-01-15", "calendar_colour": "#FF5733"}
								""".formatted(Fixture.GLOW_ADMIN_ID)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("Colorist"))
				.andExpect(jsonPath("$.user_id").value(Fixture.GLOW_ADMIN_ID))
				.andExpect(jsonPath("$.hired_at").value("2024-01-15"))
				.andExpect(jsonPath("$.calendar_colour").value("#FF5733"))
				.andExpect(jsonPath("$.first_name").value("Anna"))
				.andExpect(jsonPath("$.last_name").value("Admin"))
				.andReturn().getResponse().getHeader("Location");

		mvc.perform(get(location).session(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Colorist"));
	}

	@Test
	void creatingStaffRejectsABlankTitle() throws Exception
	{
		mvc.perform(post("/api/v1/businesses/" + Fixture.GLOW + "/staff").session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"user_id": %d, "title": "  ", "is_active": true, "hired_at": "2024-01-15"}
								""".formatted(Fixture.GLOW_ADMIN_ID)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void creatingStaffRejectsAMalformedCalendarColour() throws Exception
	{
		mvc.perform(post("/api/v1/businesses/" + Fixture.GLOW + "/staff").session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"user_id": %d, "title": "Colorist", "is_active": true, "hired_at": "2024-01-15", "calendar_colour": "blue"}
								""".formatted(Fixture.GLOW_ADMIN_ID)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void cannotHireAUserFromAnotherBusiness() throws Exception
	{
		mvc.perform(post("/api/v1/businesses/" + Fixture.GLOW + "/staff").session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"user_id": %d, "title": "Stylist", "is_active": true, "hired_at": "2024-01-15"}
								""".formatted(Fixture.URBAN_ADMIN_ID)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void cannotHireACustomer() throws Exception
	{
		// Olivia (id 5) is a CUSTOMER with no business - never employable as staff.
		mvc.perform(post("/api/v1/businesses/" + Fixture.GLOW + "/staff").session(loginAs(Fixture.SUPER_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"user_id": 5, "title": "Stylist", "is_active": true, "hired_at": "2024-01-15"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void hiringANonexistentUserIs404() throws Exception
	{
		mvc.perform(post("/api/v1/businesses/" + Fixture.GLOW + "/staff").session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"user_id": 999999, "title": "Stylist", "is_active": true, "hired_at": "2024-01-15"}
								"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void aUserCanOnlyHaveOneStaffRecord() throws Exception
	{
		// Mia (GLOW_EMPLOYEE_ID) is already staff (GLOW_STAFF) per the fixture.
		mvc.perform(post("/api/v1/businesses/" + Fixture.GLOW + "/staff").session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"user_id": %d, "title": "Stylist", "is_active": true, "hired_at": "2024-01-15"}
								""".formatted(Fixture.GLOW_EMPLOYEE_ID)))
				.andExpect(status().isConflict());
	}

	@Test
	void adminCannotHireIntoAnotherBusiness() throws Exception
	{
		mvc.perform(post("/api/v1/businesses/" + Fixture.URBAN + "/staff").session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"user_id": %d, "title": "Barber", "is_active": true, "hired_at": "2024-01-15"}
								""".formatted(Fixture.URBAN_ADMIN_ID)))
				.andExpect(status().isForbidden());
	}

	@Test
	void employeesCannotHireStaff() throws Exception
	{
		mvc.perform(post("/api/v1/businesses/" + Fixture.GLOW + "/staff").session(loginAs(Fixture.GLOW_EMPLOYEE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"user_id": %d, "title": "Stylist", "is_active": true, "hired_at": "2024-01-15"}
								""".formatted(Fixture.GLOW_ADMIN_ID)))
				.andExpect(status().isForbidden());
	}

	@Test
	void superAdminCanHireIntoAnyBusiness() throws Exception
	{
		mvc.perform(post("/api/v1/businesses/" + Fixture.URBAN + "/staff").session(loginAs(Fixture.SUPER_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"user_id": %d, "title": "Shop Manager", "is_active": true, "hired_at": "2024-01-15"}
								""".formatted(Fixture.URBAN_ADMIN_ID)))
				.andExpect(status().isCreated());
	}

	@Test
	void listingStaffReturnsTheBusinessRoster() throws Exception
	{
		mvc.perform(get("/api/v1/businesses/" + Fixture.GLOW + "/staff").session(loginAs(Fixture.GLOW_EMPLOYEE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(Fixture.GLOW_STAFF))
				.andExpect(jsonPath("$[0].user_id").value(Fixture.GLOW_EMPLOYEE_ID));
	}

	@Test
	void theRosterNamesEachStaffMemberAndShowsEmailsOnlyToTheSalonsAdmins() throws Exception
	{
		String roster = "/api/v1/businesses/" + Fixture.GLOW + "/staff";
		for (String admin : new String[] {Fixture.GLOW_ADMIN, Fixture.SUPER_ADMIN}) {
			mvc.perform(get(roster).session(loginAs(admin)))
					.andExpect(jsonPath("$[0].first_name").value("Mia"))
					.andExpect(jsonPath("$[0].last_name").value("Stylist"))
					.andExpect(jsonPath("$[0].email").value(Fixture.GLOW_EMPLOYEE));
			mvc.perform(get(roster + "/" + Fixture.GLOW_STAFF).session(loginAs(admin)))
					.andExpect(jsonPath("$.email").value(Fixture.GLOW_EMPLOYEE));
		}
		// Not another salon's admin, nor a colleague: on /users neither of them could read it either.
		for (String other : new String[] {Fixture.URBAN_ADMIN, Fixture.GLOW_EMPLOYEE}) {
			mvc.perform(get(roster).session(loginAs(other)))
					.andExpect(jsonPath("$[0].first_name").value("Mia"))
					.andExpect(jsonPath("$[0].email").value(nullValue()));
			mvc.perform(get(roster + "/" + Fixture.GLOW_STAFF).session(loginAs(other)))
					.andExpect(jsonPath("$.email").value(nullValue()));
		}
	}

	@Test
	void gettingAMissingStaffRecordIs404() throws Exception
	{
		mvc.perform(get("/api/v1/businesses/" + Fixture.GLOW + "/staff/999999").session(loginAs(Fixture.GLOW_ADMIN)))
				.andExpect(status().isNotFound());
	}

	@Test
	void aStaffRecordUnderTheWrongBusinessIs404() throws Exception
	{
		// GLOW_STAFF belongs to GLOW, not URBAN - the business in the path must actually own it.
		mvc.perform(get("/api/v1/businesses/" + Fixture.URBAN + "/staff/" + Fixture.GLOW_STAFF).session(loginAs(Fixture.GLOW_ADMIN)))
				.andExpect(status().isNotFound());
	}

	@Test
	void adminCanUpdateTheirOwnStaffRecord() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		mvc.perform(put("/api/v1/businesses/" + Fixture.GLOW + "/staff/" + Fixture.GLOW_STAFF).session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title": "Lead Stylist", "is_active": false, "hired_at": "2021-03-15"}
								"""))
				.andExpect(status().isOk());

		mvc.perform(get("/api/v1/businesses/" + Fixture.GLOW + "/staff/" + Fixture.GLOW_STAFF).session(admin))
				.andExpect(jsonPath("$.title").value("Lead Stylist"))
				.andExpect(jsonPath("$.is_active").value(false))
				.andExpect(jsonPath("$.updated_at").value(org.hamcrest.Matchers.notNullValue()));
	}

	@Test
	void adminCannotUpdateAnotherBusinesssStaffRecord() throws Exception
	{
		mvc.perform(put("/api/v1/businesses/" + Fixture.URBAN + "/staff/" + Fixture.URBAN_STAFF).session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title": "Hijacked", "is_active": true, "hired_at": "2020-11-02"}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCanFireAStaffMemberWithoutDeletingTheirAccount() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		mvc.perform(delete("/api/v1/businesses/" + Fixture.GLOW + "/staff/" + Fixture.GLOW_STAFF).session(admin))
				.andExpect(status().isOk());

		mvc.perform(get("/api/v1/businesses/" + Fixture.GLOW + "/staff/" + Fixture.GLOW_STAFF).session(admin))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/users/" + Fixture.GLOW_EMPLOYEE_ID).session(admin))
				.andExpect(status().isOk());
	}

	private static final String MIAS_SERVICES =
			"/api/v1/businesses/" + Fixture.GLOW + "/staff/" + Fixture.GLOW_STAFF + "/services";

	private static String serviceIds(long... ids)
	{
		return "{\"service_ids\": " + Arrays.toString(ids) + "}";
	}

	@Test
	void anyoneSignedInCanSeeWhichServicesAStaffMemberPerforms() throws Exception
	{
		// DB-14: public like the salon's service list, so the booking flow can offer the right people.
		mvc.perform(get(MIAS_SERVICES).session(loginAs(Fixture.URBAN_EMPLOYEE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(Fixture.GLOW_HAIRCUT))
				.andExpect(jsonPath("$[0].duration_minutes").value(45));
	}

	@Test
	void adminReplacesTheServicesAStaffMemberPerforms() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		mvc.perform(put(MIAS_SERVICES).session(admin).contentType(MediaType.APPLICATION_JSON)
						.content(serviceIds(Fixture.GLOW_HAIRCUT, Fixture.GLOW_MANICURE, Fixture.GLOW_HAIRCUT)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].name").value("Classic Manicure"))
				.andExpect(jsonPath("$[1].name").value("Signature Haircut"));

		mvc.perform(put(MIAS_SERVICES).session(admin).contentType(MediaType.APPLICATION_JSON).content(serviceIds()))
				.andExpect(status().isOk());
		mvc.perform(get(MIAS_SERVICES).session(admin)).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void aStaffMemberCannotPerformAnotherSalonsService() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		mvc.perform(put(MIAS_SERVICES).session(admin).contentType(MediaType.APPLICATION_JSON)
						.content(serviceIds(Fixture.GLOW_MANICURE, Fixture.URBAN_FADE)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Service with id 3 is not offered by this business."));
		mvc.perform(get(MIAS_SERVICES).session(admin)).andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void onlyAnAdminOfTheSalonCanChangeAStaffMembersServices() throws Exception
	{
		String body = serviceIds(Fixture.GLOW_MANICURE);

		mvc.perform(put(MIAS_SERVICES).session(loginAs(Fixture.GLOW_EMPLOYEE)).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden());
		mvc.perform(put(MIAS_SERVICES).session(loginAs(Fixture.URBAN_ADMIN)).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/businesses/" + Fixture.GLOW + "/staff/" + Fixture.URBAN_STAFF + "/services")
						.session(loginAs(Fixture.GLOW_ADMIN)))
				.andExpect(status().isNotFound());
	}
}
