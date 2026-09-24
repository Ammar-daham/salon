package com.example.salon.security;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthenticationTest extends IntegrationTest
{
	@Test
	void loginReturnsTheUserInSnakeCaseWithTheirBusiness() throws Exception
	{
		mvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody(Fixture.GLOW_ADMIN, Fixture.PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(Fixture.GLOW_ADMIN_ID))
				.andExpect(jsonPath("$.first_name").value("Anna"))
				.andExpect(jsonPath("$.last_name").value("Admin"))
				.andExpect(jsonPath("$.role").value("ADMIN"))
				.andExpect(jsonPath("$.business_id").value(Fixture.GLOW))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.password_hash").doesNotExist());
	}

	@Test
	void superAdminHasNoBusiness() throws Exception
	{
		mvc.perform(get("/api/v1/auth/me").session(loginAs(Fixture.SUPER_ADMIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("SUPER_ADMIN"))
				.andExpect(jsonPath("$.business_id").value(nullValue()));
	}

	@Test
	void wrongPasswordAndUnknownEmailGetTheSameGenericError() throws Exception
	{
		mvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody(Fixture.GLOW_ADMIN, "wrong-password")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid email or password"));

		mvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody("nobody@salon.test", Fixture.PASSWORD)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid email or password"));
	}

	@Test
	void meRequiresASession() throws Exception
	{
		mvc.perform(get("/api/v1/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
	}

	@Test
	void logoutInvalidatesTheSession() throws Exception
	{
		MockHttpSession session = loginAs(Fixture.GLOW_EMPLOYEE);
		mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isOk());

		mvc.perform(post("/api/v1/auth/logout").session(session)).andExpect(status().isOk());

		mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isUnauthorized());
	}
}
