package com.example.salon.security;

import com.example.salon.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A client creates their own account without a session, to book in the customer app. */
class SignUpTest extends IntegrationTest
{
	private static final String OLIVIA = "olivia@client.test";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void aClientSignsUpAsACustomerWithNoSalonAndThenSignsIn() throws Exception
	{
		MvcResult created = signUp(body("Olivia", OLIVIA))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.first_name").value("Olivia"))
				.andExpect(jsonPath("$.last_name").value("Weber"))
				.andExpect(jsonPath("$.email").value(OLIVIA))
				.andExpect(jsonPath("$.role").value("CUSTOMER"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.password_hash").doesNotExist())
				.andReturn();

		long id = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id"))
				.longValue();
		assertThat(created.getResponse().getHeader("Location")).endsWith("/api/v1/users/" + id);
		// Signing up doesn't sign in.
		assertThat(created.getResponse().getCookie("JSESSIONID")).isNull();

		Map<String, Object> row = jdbcTemplate.queryForMap(
				"SELECT role, business_id, password_hash FROM users WHERE id = ?", id);
		assertThat(row.get("role")).isEqualTo("CUSTOMER");
		assertThat(row.get("business_id")).isNull();
		assertThat((String) row.get("password_hash")).startsWith("$2").doesNotContain(Fixture.PASSWORD);

		mvc.perform(get("/api/v1/auth/me").session(loginAs(OLIVIA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.role").value("CUSTOMER"))
				.andExpect(jsonPath("$.business_id").value(nullValue()));
	}

	@Test
	void theAccountIsACustomersWithNoSalonWhateverTheBodySays() throws Exception
	{
		signUp(body("Olivia", OLIVIA).replace("{", "{\"role\": \"SUPER_ADMIN\", \"business_id\": 1, "))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role").value("CUSTOMER"));

		Map<String, Object> row = jdbcTemplate.queryForMap(
				"SELECT role, business_id FROM users WHERE email = ?", OLIVIA);
		assertThat(row.get("role")).isEqualTo("CUSTOMER");
		assertThat(row.get("business_id")).isNull();
	}

	@Test
	void anEmailWithAnAccountInAnyCaseIsAConflictThatCreatesNothing() throws Exception
	{
		signUp(body("Anna", Fixture.GLOW_ADMIN.toUpperCase()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Email already in use."));
		signUp(body("Olivia", OLIVIA)).andExpect(status().isCreated());
		signUp(body("Olivia", "Olivia@Client.test"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Email already in use."));

		assertThat(countUsers()).isEqualTo(7);
	}

	@Test
	void aBadSignUpNamesEachField() throws Exception
	{
		signUp(body("", "not-an-email"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", containsString("first_name must not be blank")))
				.andExpect(jsonPath("$.message", containsString("email must be a well-formed email address")));
		signUp(body("Olivia", OLIVIA).replace(Fixture.PASSWORD, "short"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("password must be between 8 and 72 characters"));
		signUp("{\"first_name\": \"Olivia\", \"last_name\": \"Weber\", \"email\": \"" + OLIVIA + "\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("password must not be blank"));

		assertThat(countUsers()).isEqualTo(6);
	}

	@Test
	void oneAddressCanSignUpOnlySoManyTimesAnHourApartFromRegisteringSalons() throws Exception
	{
		for (int i = 1; i <= 5; i++) {
			// A taken email counts too, so emails can't be tried one after another.
			String email = i == 3 ? Fixture.GLOW_ADMIN : "client" + i + "@client.test";
			signUp(body("Client", email), "203.0.113.7")
					.andExpect(i == 3 ? status().isConflict() : status().isCreated());
		}

		signUp(body("Client", "client6@client.test"), "203.0.113.7")
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"))
				.andExpect(jsonPath("$.message", containsString("Too many accounts created from this address.")));
		signUp(body("Client", "client6@client.test"), "198.51.100.4").andExpect(status().isCreated());

		// Registering a salon from the same address is counted on its own.
		mvc.perform(post("/api/v1/auth/register")
						.with(request -> {
							request.setRemoteAddr("203.0.113.7");
							return request;
						})
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"owner": {"first_name": "Sara", "last_name": "Owner", "email": "sara@lumen.test",
								           "password": "%s"},
								 "salon": {"name": "Lumen Nail Bar", "image": "https://img.test/lumen.png",
								           "currency": "EUR", "timezone": "Europe/Berlin"}}
								""".formatted(Fixture.PASSWORD)))
				.andExpect(status().isAccepted());
	}

	private ResultActions signUp(String body) throws Exception
	{
		return signUp(body, "192.0.2.1");
	}

	private ResultActions signUp(String body, String address) throws Exception
	{
		return mvc.perform(post("/api/v1/auth/sign-up")
				.with(request -> {
					request.setRemoteAddr(address);
					return request;
				})
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private int countUsers()
	{
		return jdbcTemplate.queryForObject("SELECT count(*) FROM users", Integer.class);
	}

	private static String body(String firstName, String email)
	{
		return """
				{"first_name": "%s", "last_name": "Weber", "email": "%s", "password": "%s"}
				""".formatted(firstName, email, Fixture.PASSWORD);
	}
}
