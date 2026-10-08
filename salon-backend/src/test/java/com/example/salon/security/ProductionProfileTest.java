package com.example.salon.security;

import com.example.salon.support.IntegrationTest;
import org.apache.catalina.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The prod profile, on a real Tomcat, the way it runs behind a proxy that terminates TLS. The proxy is
 * this test, on 127.0.0.1, saying with X-Forwarded-Proto whether the client came over HTTPS. Only the
 * SALON_* variables the context needs are set: the datasource comes from IntegrationTest's container.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"SALON_ALLOWED_ORIGINS=https://admin.salon.test",
		"SALON_PASSWORD_RESET_LINK=https://admin.salon.test/reset-password",
		"SALON_MAIL_FROM=Salon Admin <no-reply@salon.test>",
		"SALON_SMTP_HOST=smtp.salon.test",
		"SALON_SMTP_USERNAME=salon",
		"SALON_SMTP_PASSWORD=not-used",
		"SALON_SESSION_TIMEOUT=45m",
		// The prod profile's log file, kept out of the source tree.
		"logging.file.path=build/test-logs",
})
@ActiveProfiles("prod")
@ExtendWith(OutputCaptureExtension.class)
class ProductionProfileTest extends IntegrationTest
{
	@LocalServerPort
	private int port;

	@Autowired
	private WebServerApplicationContext context;

	private final HttpClient http = HttpClient.newBuilder()
			.proxy(HttpClient.Builder.NO_PROXY)
			.version(HttpClient.Version.HTTP_1_1)
			.build();

	@Test
	void signingInOverHttpsGivesACookieOnlyHttpsCarriesAndScriptsCantRead() throws Exception
	{
		HttpResponse<String> response = send(signIn().header("X-Forwarded-Proto", "https"));

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().allValues("Set-Cookie")).isNotEmpty().allSatisfy(cookie -> assertThat(cookie)
				.startsWith("JSESSIONID=").contains("; Secure").contains("; HttpOnly").contains("; SameSite=Lax"));
		// Browsers that saw this keep to HTTPS for a year.
		assertThat(response.headers().firstValue("Strict-Transport-Security")).hasValueSatisfying(hsts ->
				assertThat(hsts).contains("max-age="));
	}

	@Test
	void plainHttpIsRefusedBeforeAnythingHappens() throws Exception
	{
		HttpResponse<String> response = send(signIn());

		assertThat(response.statusCode()).isEqualTo(403);
		assertThat(response.body()).contains("This API only answers over HTTPS");
		assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
	}

	@Test
	void aLoadBalancerCanCheckHealthOverPlainHttp() throws Exception
	{
		for (String path : List.of("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness")) {
			HttpResponse<String> response = send(request(path).GET());

			assertThat(response.statusCode()).as(path).isEqualTo(200);
			assertThat(response.body()).as(path).contains("\"status\":\"UP\"");
		}
	}

	@Test
	void theClientsAddressIsTheOneTheProxySaw(CapturedOutput output) throws Exception
	{
		send(request("/api/v1/auth/me").GET()
				.header("X-Forwarded-Proto", "https")
				.header("X-Forwarded-For", "203.0.113.7"));

		// What the request log and the sign-in limits see (BE-22), rather than the proxy's 127.0.0.1.
		assertThat(output).containsPattern("GET /api/v1/auth/me -> 401 .*ip=203\\.0\\.113\\.7\\)");
	}

	@Test
	void howLongASessionLastsWithoutARequestIsSetByTheEnvironment()
	{
		Context app = (Context) ((TomcatWebServer) context.getWebServer()).getTomcat().getHost().findChildren()[0];

		assertThat(app.getSessionTimeout()).isEqualTo(45);
	}

	private HttpRequest.Builder signIn()
	{
		return request("/api/v1/auth/login")
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(loginBody(Fixture.GLOW_ADMIN, Fixture.PASSWORD)));
	}

	private HttpRequest.Builder request(String path)
	{
		return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
	}

	private HttpResponse<String> send(HttpRequest.Builder request) throws IOException, InterruptedException
	{
		return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}
}
