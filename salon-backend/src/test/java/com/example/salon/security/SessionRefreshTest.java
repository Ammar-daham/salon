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
 * BE-14: a signed-in session must follow changes to the user, not keep the snapshot taken at login.
 */
class SessionRefreshTest extends IntegrationTest
{
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void aDemotedAdminLosesAdminAccessOnTheirExistingSession() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		mvc.perform(get("/api/v1/users").session(admin)).andExpect(status().isOk());

		mvc.perform(put("/api/v1/users/" + Fixture.GLOW_ADMIN_ID).session(loginAs(Fixture.SUPER_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name": "Anna", "last_name": "Admin", "role": "EMPLOYEE"}
								"""))
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
								{"first_name": "Mia", "last_name": "Stylist", "role": "EMPLOYEE"}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void aDeletedUsersSessionIsInvalidated() throws Exception
	{
		MockHttpSession employee = loginAs(Fixture.GLOW_EMPLOYEE);
		mvc.perform(get("/api/v1/auth/me").session(employee)).andExpect(status().isOk());

		mvc.perform(delete("/api/v1/users/" + Fixture.GLOW_EMPLOYEE_ID).session(loginAs(Fixture.SUPER_ADMIN)))
				.andExpect(status().isOk());

		mvc.perform(get("/api/v1/auth/me").session(employee))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
		assertThat(employee.isInvalid()).isTrue();
	}
}
