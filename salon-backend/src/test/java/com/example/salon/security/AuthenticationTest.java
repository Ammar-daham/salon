package com.example.salon.security;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

	/** For the customer app. It's the admin panel that turns a CUSTOMER away. */
	@Test
	void aCustomerWithAPasswordCanSignIn() throws Exception
	{
		mvc.perform(post("/api/v1/users").session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name": "Ella", "last_name": "Client", "role": "CUSTOMER",
								 "email": "ella@client.test", "password": "%s"}
								""".formatted(Fixture.PASSWORD)))
				.andExpect(status().isCreated());

		mvc.perform(get("/api/v1/auth/me").session(loginAs("ella@client.test")))
				.andExpect(jsonPath("$.role").value("CUSTOMER"));
	}

	/**
	 * A salon's staff wait for a super admin to approve it, and are told why. Only once the password matches,
	 * so no one else learns where the salon stands, and without a session or a failed sign-in counted.
	 */
	@Test
	void aSalonsStaffCanSignInOnlyWhileItIsApprovedOrSuspended() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);
		mvc.perform(post("/api/v1/users").session(superAdmin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name": "Sara", "last_name": "Owner", "role": "ADMIN", "business_id": %d,
								 "email": "sara@serenity.test", "password": "%s"}
								""".formatted(Fixture.SERENITY_PENDING, Fixture.PASSWORD)))
				.andExpect(status().isCreated());

		// Serenity is pending. Asking again and again isn't a failed sign-in, so it doesn't lock her out.
		for (int i = 0; i < 6; i++) {
			MvcResult refused = signIn("sara@serenity.test", Fixture.PASSWORD)
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.message")
							.value("Your salon is waiting for approval. You can sign in once it's approved."))
					.andReturn();
			assertThat(refused.getRequest().getSession(false)).isNull();
		}
		signIn("sara@serenity.test", "wrong-password").andExpect(status().isUnauthorized());

		setStatus(superAdmin, "REJECTED");
		signIn("sara@serenity.test", Fixture.PASSWORD)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Your salon's registration wasn't approved."));

		setStatus(superAdmin, "APPROVED");
		mvc.perform(get("/api/v1/auth/me").session(loginAs("sara@serenity.test")))
				.andExpect(jsonPath("$.business_id").value(Fixture.SERENITY_PENDING));

		setStatus(superAdmin, "SUSPENDED");
		signIn("sara@serenity.test", Fixture.PASSWORD).andExpect(status().isOk());
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
	void loginEmailIsCaseInsensitive() throws Exception
	{
		// Anna@x and anna@x are one account, so either spelling signs in.
		mvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody(Fixture.GLOW_ADMIN.toUpperCase(), Fixture.PASSWORD)))
				.andExpect(status().isOk());
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

	@Test
	void loginRotatesThePreExistingSessionId() throws Exception
	{
		MockHttpSession session = new MockHttpSession();
		String anonymousId = session.getId();

		MvcResult result = mvc.perform(post("/api/v1/auth/login").session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody(Fixture.GLOW_EMPLOYEE, Fixture.PASSWORD)))
				.andExpect(status().isOk())
				.andReturn();

		assertThat(result.getRequest().getSession(false).getId()).isNotEqualTo(anonymousId);
	}

	private ResultActions signIn(String email, String password) throws Exception
	{
		return mvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody(email, password)));
	}

	private void setStatus(MockHttpSession superAdmin, String status) throws Exception
	{
		mvc.perform(put("/api/v1/businesses/" + Fixture.SERENITY_PENDING + "/status").session(superAdmin)
						.contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"" + status + "\"}"))
				.andExpect(status().isOk());
	}
}
