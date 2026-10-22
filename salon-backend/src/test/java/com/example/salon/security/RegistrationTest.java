package com.example.salon.security;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A salon owner registers their salon and account without a session, for a super admin to review. */
class RegistrationTest extends IntegrationTest
{
	private static final String SARA = "sara@lumen.test";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void aRegisteredSalonWaitsForReviewWithItsOwnerAsItsAdmin() throws Exception
	{
		register(body("Lumen Nail Bar", SARA))
				.andExpect(status().isAccepted())
				.andExpect(content().string(""));

		long lumen = idOf("Lumen Nail Bar");
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);
		mvc.perform(get("/api/v1/businesses/" + lumen).session(superAdmin))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.addresses[0].city").value("Berlin"))
				.andExpect(jsonPath("$.contacts[0].value").value("+49 30 5550100"));
		mvc.perform(get("/api/v1/users?business_id=" + lumen).session(superAdmin))
				.andExpect(jsonPath("$.items[0].email").value(SARA))
				.andExpect(jsonPath("$.items[0].role").value("ADMIN"))
				.andExpect(jsonPath("$.total_items").value(1));
		mvc.perform(get("/api/v1/notifications?unread=true").session(superAdmin))
				.andExpect(jsonPath("$.items[0].type").value("BUSINESS_REGISTERED"))
				.andExpect(jsonPath("$.items[0].business.id").value(lumen))
				.andExpect(jsonPath("$.items[0].business.name").value("Lumen Nail Bar"));
		// Another salon's admin doesn't see it while it's pending.
		mvc.perform(get("/api/v1/businesses/" + lumen).session(loginAs(Fixture.GLOW_ADMIN)))
				.andExpect(status().isNotFound());

		signIn(SARA)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message")
						.value("Your salon is waiting for approval. You can sign in once it's approved."));

		mvc.perform(put("/api/v1/businesses/" + lumen + "/status").session(superAdmin)
						.contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"APPROVED\"}"))
				.andExpect(status().isOk());
		mvc.perform(get("/api/v1/auth/me").session(loginAs(SARA)))
				.andExpect(jsonPath("$.role").value("ADMIN"))
				.andExpect(jsonPath("$.business_id").value(lumen));
	}

	@Test
	void everySuperAdminIsToldAndNoOneElse() throws Exception
	{
		jdbcTemplate.update("""
				INSERT INTO users (first_name, last_name, role, email, password_hash)
				SELECT 'Kim', 'Root', 'SUPER_ADMIN', 'root2@salon.test', password_hash FROM users WHERE id = ?
				""", Fixture.SUPER_ADMIN_ID);

		register(body("Lumen Nail Bar", SARA)).andExpect(status().isAccepted());

		for (String superAdmin : new String[] {Fixture.SUPER_ADMIN, "root2@salon.test"}) {
			mvc.perform(get("/api/v1/notifications/unread-count").session(loginAs(superAdmin)))
					.andExpect(jsonPath("$.count").value(1));
		}
		mvc.perform(get("/api/v1/notifications/unread-count").session(loginAs(Fixture.GLOW_ADMIN)))
				.andExpect(jsonPath("$.count").value(0));
	}

	@Test
	void aTakenNameOrEmailOrATwiceGivenContactIsAConflictThatCreatesNothing() throws Exception
	{
		register(body("Glow Beauty Studio", SARA))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Business with name Glow Beauty Studio already exists."));
		// The salon is inserted first, so a taken email has to undo it.
		register(body("Lumen Nail Bar", Fixture.GLOW_ADMIN.toUpperCase()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Email already in use."));
		register(body("Lumen Nail Bar", SARA).replace("+49 30 5550100\"}]",
						"+49 30 5550100\"}, {\"type\": \"phone\", \"value\": \"+49 30 5550100\"}]"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Contact already exists."));

		assertThat(count("businesses")).isEqualTo(3);
		assertThat(count("users")).isEqualTo(6);
		assertThat(count("addresses WHERE business_id IS NOT NULL")).isEqualTo(1);
		assertThat(count("notifications")).isZero();
	}

	@Test
	void aRegisteredSalonIsPendingWhateverStatusItAsksFor() throws Exception
	{
		register(body("Lumen Nail Bar", SARA).replace("\"timezone\":", "\"status\": \"APPROVED\", \"timezone\":"))
				.andExpect(status().isAccepted());

		assertThat(jdbcTemplate.queryForObject("SELECT status FROM businesses WHERE name = 'Lumen Nail Bar'",
				String.class)).isEqualTo("PENDING");
		signIn(SARA).andExpect(status().isForbidden());
	}

	@Test
	void aBadRegistrationNamesEachFieldByItsPath() throws Exception
	{
		register(body("", "not-an-email"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", containsString("owner.email must be a well-formed email address")))
				.andExpect(jsonPath("$.message", containsString("salon.name must not be blank")));
		register(body("Lumen Nail Bar", SARA).replace(Fixture.PASSWORD, "short"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("owner.password must be between 8 and 72 characters"));
		register("{\"salon\": {\"name\": \"Lumen Nail Bar\", \"image\": \"x\"}}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("owner must not be null"));

		assertThat(count("businesses")).isEqualTo(3);
	}

	@Test
	void oneAddressCanRegisterOnlySoManySalonsAnHour() throws Exception
	{
		for (int i = 1; i <= 5; i++) {
			// A conflict counts too, so names and emails can't be tried one after another.
			String name = i == 3 ? "Glow Beauty Studio" : "Salon " + i;
			register(body(name, "owner" + i + "@salon.test"), "203.0.113.7")
					.andExpect(i == 3 ? status().isConflict() : status().isAccepted());
		}

		register(body("Salon 6", "owner6@salon.test"), "203.0.113.7")
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"))
				.andExpect(jsonPath("$.message", containsString("Too many salons registered from this address.")));
		register(body("Salon 6", "owner6@salon.test"), "198.51.100.4").andExpect(status().isAccepted());

		mvc.perform(get("/api/v1/businesses?status=PENDING&size=100").session(loginAs(Fixture.SUPER_ADMIN)))
				.andExpect(jsonPath("$.items[*].name", hasItem("Salon 6")))
				.andExpect(jsonPath("$.total_items").value(6));
	}

	private ResultActions register(String body) throws Exception
	{
		return register(body, "192.0.2.1");
	}

	private ResultActions register(String body, String address) throws Exception
	{
		return mvc.perform(post("/api/v1/auth/register")
				.with(request -> {
					request.setRemoteAddr(address);
					return request;
				})
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private ResultActions signIn(String email) throws Exception
	{
		return mvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody(email, Fixture.PASSWORD)));
	}

	private long idOf(String salon)
	{
		return jdbcTemplate.queryForObject("SELECT id FROM businesses WHERE name = ?", Long.class, salon);
	}

	private int count(String tableAndWhere)
	{
		return jdbcTemplate.queryForObject("SELECT count(*) FROM " + tableAndWhere, Integer.class);
	}

	private static String body(String salon, String ownerEmail)
	{
		return """
				{"owner": {"first_name": "Sara", "last_name": "Owner", "email": "%s", "password": "%s"},
				 "salon": {"name": "%s", "description": "Nails and lashes.", "image": "https://img.test/lumen.png",
				           "currency": "EUR", "timezone": "Europe/Berlin",
				           "addresses": [{"street": "5 Torstr.", "city": "Berlin", "country": "Germany", "postal_code": "10119"}],
				           "contacts": [{"type": "phone", "value": "+49 30 5550100"}]}}
				""".formatted(ownerEmail, Fixture.PASSWORD, salon);
	}
}
