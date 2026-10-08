package com.example.salon.security;

import com.example.salon.logging.RequestLog;
import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The admin panel calls from its own origin, with the session cookie. CORS lets it, with only the
 * headers it sends (BE-29), and is also what stops a page on any other origin from using a signed-in
 * user's session, since there's no CSRF token (BE-28).
 */
class CrossOriginTest extends IntegrationTest
{
	/** app.cors.allowed-origins' default. */
	private static final String PANEL = "http://localhost:3000";
	private static final String OTHER_SITE = "https://other-site.example";

	@Test
	void thePanelMaySendJsonAndARequestIdWithTheSessionCookie() throws Exception
	{
		mvc.perform(options("/api/v1/businesses/" + Fixture.GLOW + "/customers")
						.header(HttpHeaders.ORIGIN, PANEL)
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type, accept, x-request-id"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PANEL))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsStringIgnoringCase("content-type")))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
						containsStringIgnoringCase(RequestLog.REQUEST_ID_HEADER)));
	}

	/** The preflight answer leaves the header out, and the browser then doesn't send the request. */
	@Test
	void aHeaderThePanelDoesntSendIsNotAllowed() throws Exception
	{
		mvc.perform(options("/api/v1/businesses/" + Fixture.GLOW + "/customers")
						.header(HttpHeaders.ORIGIN, PANEL)
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type, x-anything"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
						not(containsStringIgnoringCase("x-anything"))));
		mvc.perform(options("/api/v1/businesses/" + Fixture.GLOW + "/customers")
						.header(HttpHeaders.ORIGIN, PANEL)
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "x-anything"))
				.andExpect(status().isForbidden());
	}

	@Test
	void anotherSiteCantUseTheSessionEvenWithRequestsThatSkipThePreflight() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		// What a form or fetch() on another site can send without asking first: the browser adds the
		// cookie, if SameSite lets it, and always the Origin.
		mvc.perform(post("/api/v1/auth/logout").session(admin).header(HttpHeaders.ORIGIN, OTHER_SITE))
				.andExpect(status().isForbidden());
		mvc.perform(post("/api/v1/auth/change-password").session(admin).header(HttpHeaders.ORIGIN, OTHER_SITE)
						.contentType(MediaType.TEXT_PLAIN)
						.content("""
								{"current_password": "%s", "new_password": "Taken-Over-1"}
								""".formatted(Fixture.PASSWORD)))
				.andExpect(status().isForbidden());

		mvc.perform(get("/api/v1/auth/me").session(admin)).andExpect(status().isOk());
		loginAs(Fixture.GLOW_ADMIN);
	}

	@Test
	void thePanelsOwnWritesGoThrough() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		mvc.perform(post("/api/v1/auth/logout").session(admin).header(HttpHeaders.ORIGIN, PANEL))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PANEL));
	}

	@Test
	void allowingEveryOriginStopsStartup()
	{
		SecurityConfig config = new SecurityConfig(null, null);
		ReflectionTestUtils.setField(config, "allowedOrigins", "http://localhost:3000, *");

		assertThatThrownBy(config::corsConfigurationSource).isInstanceOf(IllegalArgumentException.class);
	}
}
