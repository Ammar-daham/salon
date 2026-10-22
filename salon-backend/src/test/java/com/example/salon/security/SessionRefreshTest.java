package com.example.salon.security;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A signed-in session must follow changes to the user, not keep the snapshot taken at login.
 */
class SessionRefreshTest extends IntegrationTest
{
	@Autowired
	private JdbcTemplate jdbcTemplate;

	/** A salon put back to pending, or rejected, signs its staff out; a suspended one doesn't. */
	@Test
	void aSalonThatStopsBeingApprovedSignsItsStaffOut() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);
		MockHttpSession glowAdmin = loginAs(Fixture.GLOW_ADMIN);
		MockHttpSession urbanAdmin = loginAs(Fixture.URBAN_ADMIN);

		setStatus(superAdmin, Fixture.GLOW, "REJECTED");
		setStatus(superAdmin, Fixture.URBAN, "SUSPENDED");

		mvc.perform(get("/api/v1/auth/me").session(glowAdmin)).andExpect(status().isUnauthorized());
		assertThat(glowAdmin.isInvalid()).isTrue();
		mvc.perform(get("/api/v1/auth/me").session(urbanAdmin)).andExpect(status().isOk());
		mvc.perform(get("/api/v1/auth/me").session(superAdmin)).andExpect(status().isOk());
	}

	@Test
	void aDemotedAdminLosesAdminAccessOnTheirExistingSession() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		mvc.perform(get("/api/v1/users").session(admin)).andExpect(status().isOk());

		mvc.perform(put("/api/v1/users/" + Fixture.GLOW_ADMIN_ID + "/role").session(loginAs(Fixture.SUPER_ADMIN))
						.contentType(MediaType.APPLICATION_JSON).content("{\"role\": \"EMPLOYEE\"}"))
				.andExpect(status().isOk());

		mvc.perform(get("/api/v1/auth/me").session(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("EMPLOYEE"));
		mvc.perform(get("/api/v1/users").session(admin)).andExpect(status().isForbidden());
	}

	@Test
	void aMovedAdminLosesAccessToTheirOldBusinessOnTheirExistingSession() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		// No endpoint moves a user between businesses yet, so change the row directly.
		jdbcTemplate.update("UPDATE users SET business_id = ? WHERE id = ?", Fixture.URBAN, Fixture.GLOW_ADMIN_ID);

		mvc.perform(get("/api/v1/auth/me").session(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.business_id").value(Fixture.URBAN));
		mvc.perform(put("/api/v1/users/" + Fixture.GLOW_EMPLOYEE_ID).session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name": "Mia", "last_name": "Stylist"}
								"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void aDeletedUsersSessionIsInvalidated() throws Exception
	{
		MockHttpSession employee = loginAs(Fixture.GLOW_EMPLOYEE);
		mvc.perform(get("/api/v1/auth/me").session(employee)).andExpect(status().isOk());

		mvc.perform(delete("/api/v1/users/" + Fixture.GLOW_EMPLOYEE_ID).session(loginAs(Fixture.SUPER_ADMIN)))
				.andExpect(status().isNoContent());

		mvc.perform(get("/api/v1/auth/me").session(employee))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
		assertThat(employee.isInvalid()).isTrue();
	}

	private void setStatus(MockHttpSession superAdmin, long businessId, String status) throws Exception
	{
		mvc.perform(put("/api/v1/businesses/" + businessId + "/status").session(superAdmin)
						.contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"" + status + "\"}"))
				.andExpect(status().isOk());
	}
}
