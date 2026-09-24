package com.example.salon.security;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rules that hold today. Open findings (tenant isolation, BE-41) are in {@link KnownIssuesTest}. */
class AuthorizationRulesTest extends IntegrationTest
{
	@Test
	void anonymousRequestsAreRejected() throws Exception
	{
		mvc.perform(get("/api/v1/businesses")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/users/" + Fixture.GLOW_EMPLOYEE_ID)).andExpect(status().isUnauthorized());
	}

	@Test
	void employeesCannotWriteBusinessesOrServices() throws Exception
	{
		MockHttpSession employee = loginAs(Fixture.GLOW_EMPLOYEE);

		mvc.perform(post("/api/v1/businesses").session(employee)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Rogue Salon", "description": "x", "image": "x"}
								"""))
				.andExpect(status().isForbidden());

		mvc.perform(post("/api/v1/businesses/" + Fixture.GLOW + "/services").session(employee)
						.contentType(MediaType.APPLICATION_JSON)
						.content(serviceBody("Free Haircut")))
				.andExpect(status().isForbidden());
	}

	@Test
	void onlyAdminsCanListAllUsers() throws Exception
	{
		mvc.perform(get("/api/v1/users").session(loginAs(Fixture.GLOW_EMPLOYEE)))
				.andExpect(status().isForbidden());
	}

	@Test
	void usersCanReadThemselvesButNotOthers() throws Exception
	{
		MockHttpSession employee = loginAs(Fixture.GLOW_EMPLOYEE);

		mvc.perform(get("/api/v1/users/" + Fixture.GLOW_EMPLOYEE_ID).session(employee))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.first_name").value("Mia"));
		mvc.perform(get("/api/v1/users/" + Fixture.GLOW_ADMIN_ID).session(employee))
				.andExpect(status().isForbidden());
	}

	@Test
	void strangersCannotReadPersonalAddressesOrContacts() throws Exception
	{
		MockHttpSession stranger = loginAs(Fixture.URBAN_EMPLOYEE);

		mvc.perform(get("/api/v1/addresses/" + Fixture.GLOW_EMPLOYEE_PERSONAL_ADDRESS).session(stranger))
				.andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/contacts/" + Fixture.GLOW_EMPLOYEE_PERSONAL_CONTACT).session(stranger))
				.andExpect(status().isForbidden());
	}

	@Test
	void nonAdminSelfUpdateCannotChangeTheirOwnRole() throws Exception
	{
		MockHttpSession employee = loginAs(Fixture.GLOW_EMPLOYEE);

		mvc.perform(put("/api/v1/users/" + Fixture.GLOW_EMPLOYEE_ID).session(employee)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name": "Mia", "last_name": "Stylist", "role": "ADMIN"}
								"""))
				.andExpect(status().isOk());

		mvc.perform(get("/api/v1/users/" + Fixture.GLOW_EMPLOYEE_ID).session(employee))
				.andExpect(jsonPath("$.role").value("EMPLOYEE"));
	}

	@Test
	void adminsCannotMintSuperAdmins() throws Exception
	{
		mvc.perform(post("/api/v1/users").session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content(newUserBody("root2@salon.test", "SUPER_ADMIN", null)))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCannotPromoteThemselvesToSuperAdmin() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		mvc.perform(put("/api/v1/users/" + Fixture.GLOW_ADMIN_ID).session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name": "Anna", "last_name": "Admin", "role": "SUPER_ADMIN"}
								"""))
				.andExpect(status().isForbidden());

		mvc.perform(get("/api/v1/users/" + Fixture.GLOW_ADMIN_ID).session(admin))
				.andExpect(jsonPath("$.role").value("ADMIN"));
	}

	@Test
	void superAdminMustSayWhichBusinessANewUserBelongsTo() throws Exception
	{
		mvc.perform(post("/api/v1/users").session(loginAs(Fixture.SUPER_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content(newUserBody("new.employee@salon.test", "EMPLOYEE", null)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void adminCreatedUsersAreForcedIntoTheAdminsOwnBusiness() throws Exception
	{
		mvc.perform(post("/api/v1/users").session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content(newUserBody("new.stylist@glow.test", "EMPLOYEE", Fixture.URBAN)))
				.andExpect(status().isOk());

		mvc.perform(get("/api/v1/auth/me").session(loginAs("new.stylist@glow.test")))
				.andExpect(jsonPath("$.business_id").value(Fixture.GLOW));
	}

	@Test
	void duplicateEmailIsAConflict() throws Exception
	{
		mvc.perform(post("/api/v1/users").session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content(newUserBody(Fixture.GLOW_EMPLOYEE, "EMPLOYEE", null)))
				.andExpect(status().isConflict());
	}

	@Test
	void adminCannotApproveAnotherSalon() throws Exception
	{
		mvc.perform(put("/api/v1/businesses/" + Fixture.SERENITY_PENDING).session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Serenity Day Spa", "description": "pwned", "status": "APPROVED"}
								"""))
				.andExpect(status().isForbidden());

		mvc.perform(get("/api/v1/businesses/" + Fixture.SERENITY_PENDING).session(loginAs(Fixture.SUPER_ADMIN)))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.description").value("Massage and facials."));
	}

	@Test
	void adminCannotChangeEvenTheirOwnBusinessStatus() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		mvc.perform(put("/api/v1/businesses/" + Fixture.GLOW).session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Glow Beauty Studio", "status": "SUSPENDED"}
								"""))
				.andExpect(status().isForbidden());

		mvc.perform(get("/api/v1/businesses/" + Fixture.GLOW).session(admin))
				.andExpect(jsonPath("$.status").value("APPROVED"));
	}

	@Test
	void adminCanEditTheirOwnBusinessWhenResendingItsCurrentStatus() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		// The admin panel echoes the whole record back, including the unchanged status. That must
		// still succeed - only an actual status transition is reserved for SUPER_ADMIN.
		mvc.perform(put("/api/v1/businesses/" + Fixture.GLOW).session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Glow Beauty Studio", "description": "Now with a lash bar.", "status": "APPROVED"}
								"""))
				.andExpect(status().isOk());

		mvc.perform(get("/api/v1/businesses/" + Fixture.GLOW).session(admin))
				.andExpect(jsonPath("$.description").value("Now with a lash bar."));
	}

	@Test
	void adminCannotDeleteAnotherSalon() throws Exception
	{
		mvc.perform(delete("/api/v1/businesses/" + Fixture.URBAN).session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Urban Cuts Barbershop", "description": "Fades and beard work."}
								"""))
				.andExpect(status().isForbidden());

		mvc.perform(get("/api/v1/businesses/" + Fixture.URBAN).session(loginAs(Fixture.URBAN_ADMIN)))
				.andExpect(status().isOk());
	}

	@Test
	void superAdminCanApproveASalon() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);

		mvc.perform(put("/api/v1/businesses/" + Fixture.SERENITY_PENDING).session(superAdmin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Serenity Day Spa", "description": "Massage and facials.", "status": "APPROVED"}
								"""))
				.andExpect(status().isOk());

		mvc.perform(get("/api/v1/businesses/" + Fixture.SERENITY_PENDING).session(superAdmin))
				.andExpect(jsonPath("$.status").value("APPROVED"));
	}

	static String serviceBody(String name)
	{
		return """
				{"name": "%s", "description": "x", "duration_minutes": 30, "price": 10.00, "is_active": true}
				""".formatted(name);
	}

	static String newUserBody(String email, String role, Long businessId)
	{
		return """
				{"first_name": "New", "last_name": "User", "email": "%s", "password": "%s", "role": "%s"%s}
				""".formatted(email, Fixture.PASSWORD, role,
				businessId == null ? "" : ", \"business_id\": " + businessId);
	}
}
