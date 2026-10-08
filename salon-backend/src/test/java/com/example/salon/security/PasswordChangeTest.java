package com.example.salon.security;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FE-13: a signed-in user changes their own password. */
class PasswordChangeTest extends IntegrationTest
{
	private static final String NEW_PASSWORD = "NewPassword456!";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void theNewPasswordSignsInAndTheOldOneNoLonger() throws Exception
	{
		changePassword(loginAs(Fixture.GLOW_EMPLOYEE), Fixture.PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

		signIn(Fixture.GLOW_EMPLOYEE, Fixture.PASSWORD).andExpect(status().isUnauthorized());
		signIn(Fixture.GLOW_EMPLOYEE, NEW_PASSWORD).andExpect(status().isOk());
	}

	@Test
	void thisSessionStaysSignedInUnderANewIdAndEveryOtherIsSignedOut() throws Exception
	{
		MockHttpSession here = loginAs(Fixture.GLOW_EMPLOYEE);
		MockHttpSession elsewhere = loginAs(Fixture.GLOW_EMPLOYEE);
		String idBefore = here.getId();

		MvcResult result = changePassword(here, Fixture.PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent()).andReturn();

		assertThat(result.getRequest().getSession(false).getId()).isNotEqualTo(idBefore);
		mvc.perform(get("/api/v1/auth/me").session(here)).andExpect(status().isOk());
		mvc.perform(get("/api/v1/auth/me").session(elsewhere)).andExpect(status().isUnauthorized());
		assertThat(elsewhere.isInvalid()).isTrue();
	}

	@Test
	void aPasswordChangedAnyOtherWaySignsOutEverySession() throws Exception
	{
		MockHttpSession session = loginAs(Fixture.GLOW_EMPLOYEE);

		// The same password, hashed again: BCrypt's salt makes it a different hash, so it still counts.
		jdbcTemplate.update("UPDATE users SET password_hash = ? WHERE id = ?",
				passwordEncoder.encode(Fixture.PASSWORD), Fixture.GLOW_EMPLOYEE_ID);

		mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isUnauthorized());
	}

	@Test
	void aWrongCurrentPasswordChangesNothing() throws Exception
	{
		MockHttpSession session = loginAs(Fixture.GLOW_EMPLOYEE);

		// A 400, not a 401: the admin panel signs out on any 401.
		changePassword(session, "wrong-password", NEW_PASSWORD)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Current password is incorrect"));

		signIn(Fixture.GLOW_EMPLOYEE, Fixture.PASSWORD).andExpect(status().isOk());
		mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isOk());
	}

	@Test
	void wrongCurrentPasswordsCountAsFailedSignIns() throws Exception
	{
		MockHttpSession session = loginAs(Fixture.GLOW_EMPLOYEE);
		for (int i = 0; i < 5; i++) {
			changePassword(session, "wrong-password", NEW_PASSWORD).andExpect(status().isBadRequest());
		}

		changePassword(session, Fixture.PASSWORD, NEW_PASSWORD).andExpect(status().isTooManyRequests());
		signIn(Fixture.GLOW_EMPLOYEE, Fixture.PASSWORD).andExpect(status().isTooManyRequests());
	}

	@Test
	void theNewPasswordFollowsThePasswordRule() throws Exception
	{
		MockHttpSession session = loginAs(Fixture.GLOW_EMPLOYEE);

		changePassword(session, Fixture.PASSWORD, "short")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("newPassword must be between 8 and 72 characters"));
		changePassword(session, Fixture.PASSWORD, "x".repeat(73)).andExpect(status().isBadRequest());

		signIn(Fixture.GLOW_EMPLOYEE, Fixture.PASSWORD).andExpect(status().isOk());
	}

	@Test
	void itNeedsASession() throws Exception
	{
		mvc.perform(post("/api/v1/auth/change-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content(changeBody(Fixture.PASSWORD, NEW_PASSWORD)))
				.andExpect(status().isUnauthorized());
	}

	private ResultActions changePassword(MockHttpSession session, String current, String next) throws Exception
	{
		return mvc.perform(post("/api/v1/auth/change-password").session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(changeBody(current, next)));
	}

	private ResultActions signIn(String email, String password) throws Exception
	{
		return mvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody(email, password)));
	}

	private static String changeBody(String current, String next)
	{
		return """
				{"current_password": "%s", "new_password": "%s"}
				""".formatted(current, next);
	}
}
