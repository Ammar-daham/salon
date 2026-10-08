package com.example.salon.logging;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Each request is one log line saying who asked for what and what they got back. */
@ExtendWith(OutputCaptureExtension.class)
class RequestLoggingTest extends IntegrationTest
{
	/**
	 * Only the application's own log lines. The captured output also holds MockMvc's request/response
	 * dump, which Spring prints when any test in the class fails, and that dump does show bodies.
	 */
	private static String logLines(CapturedOutput output)
	{
		return output.getAll().lines()
				.filter(line -> line.matches("^\\d{4}-\\d{2}-\\d{2}T.*"))
				.collect(Collectors.joining("\n"));
	}

	@Test
	void aRequestIsLoggedWithItsCallerStatusAndRequestId(CapturedOutput output) throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		String requestId = mvc.perform(get("/api/v1/businesses/" + Fixture.GLOW).session(admin))
				.andExpect(status().isOk())
				.andReturn().getResponse().getHeader(RequestLog.REQUEST_ID_HEADER);

		assertThat(requestId).isNotBlank();
		assertThat(output).containsPattern("INFO \\[" + requestId + "].*"
				+ "GET /api/v1/businesses/1 -> 200 in \\d+ ms \\(user=2 ADMIN, ip=");
	}

	@Test
	void aSafeCallerSuppliedRequestIdIsReusedAndAnythingElseIsReplaced() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		mvc.perform(get("/api/v1/auth/me").session(admin).header(RequestLog.REQUEST_ID_HEADER, "panel-7f3a"))
				.andExpect(header().string(RequestLog.REQUEST_ID_HEADER, "panel-7f3a"));

		// A forged id could inject fake lines into the log, so it's never echoed.
		String forged = mvc.perform(get("/api/v1/auth/me").session(admin)
						.header(RequestLog.REQUEST_ID_HEADER, "x INFO fake line"))
				.andReturn().getResponse().getHeader(RequestLog.REQUEST_ID_HEADER);
		assertThat(forged).isNotEqualTo("x INFO fake line").matches("[0-9a-f-]{36}");
	}

	@Test
	void aFailedRequestLogsTheErrorTheClientWasSent(CapturedOutput output) throws Exception
	{
		mvc.perform(delete("/api/v1/businesses/" + Fixture.GLOW).session(loginAs(Fixture.SUPER_ADMIN)))
				.andExpect(status().isConflict());

		assertThat(output).containsPattern("WARN \\[[^]]+].*DELETE /api/v1/businesses/1 -> 409 in \\d+ ms "
				+ "\\(user=1 SUPER_ADMIN, ip=[^)]*\\) error=CONFLICT \"Business with id 1 still has staff");
	}

	@Test
	void requestsTheSecurityFiltersRejectAreLoggedToo(CapturedOutput output) throws Exception
	{
		mvc.perform(get("/api/v1/businesses")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/v1/businesses").session(loginAs(Fixture.GLOW_EMPLOYEE))
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isForbidden());

		assertThat(output).containsPattern("GET /api/v1/businesses -> 401 .*\\(user=anonymous, .*error=UNAUTHORIZED");
		assertThat(output).containsPattern("POST /api/v1/businesses -> 403 .*\\(user=4 EMPLOYEE, .*error=FORBIDDEN");
	}

	@Test
	void signingInLogsWhoButNeverThePasswordOrFullEmail(CapturedOutput output) throws Exception
	{
		loginAs(Fixture.GLOW_ADMIN);
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content(loginBody(Fixture.GLOW_ADMIN, "wrong-password")))
				.andExpect(status().isUnauthorized());

		assertThat(output).contains("User 2 signed in");
		assertThat(output).containsPattern("POST /api/v1/auth/login -> 200 .*\\(user=2 ADMIN, ");
		assertThat(output).contains("Failed sign-in for a***@glow.test");
		assertThat(logLines(output)).doesNotContain(Fixture.PASSWORD, "wrong-password", Fixture.GLOW_ADMIN);
	}

	@Test
	void signingOutIsLoggedAsTheUserWhoLeft(CapturedOutput output) throws Exception
	{
		mvc.perform(post("/api/v1/auth/logout").session(loginAs(Fixture.GLOW_ADMIN)));

		assertThat(output).contains("User 2 signed out");
		assertThat(output).containsPattern("POST /api/v1/auth/logout -> 200 .*\\(user=2 ADMIN, ");
	}

	@Test
	void whatTheRequestChangedIsLoggedUnderTheSameRequestId(CapturedOutput output) throws Exception
	{
		String requestId = mvc.perform(post("/api/v1/businesses/" + Fixture.GLOW + "/customers")
						.session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name": "Ella", "last_name": "Walk-in", "email": "ella@example.test"}
								"""))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader(RequestLog.REQUEST_ID_HEADER);

		assertThat(output).containsPattern("\\[" + requestId + "].*Created customer \\d+ in business 1");
		assertThat(logLines(output)).doesNotContain("ella@example.test");
	}

	@Test
	void queryValuesAreNotLoggedOnlyTheirNames(CapturedOutput output) throws Exception
	{
		mvc.perform(get("/api/v1/businesses?q=s3cret-search").session(loginAs(Fixture.GLOW_ADMIN)));

		assertThat(output).contains("GET /api/v1/businesses?q -> 200");
		assertThat(logLines(output)).doesNotContain("s3cret-search");
	}

	@Test
	void passingHealthChecksStayOutOfTheLog(CapturedOutput output) throws Exception
	{
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());

		assertThat(logLines(output)).doesNotContain("/actuator/health");
	}
}
