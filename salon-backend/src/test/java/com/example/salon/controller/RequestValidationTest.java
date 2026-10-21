package com.example.salon.controller;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Request bodies are now bound onto per-endpoint DTOs with Bean Validation, instead of
 * straight onto the domain models. One test per rule that DTO is responsible for.
 */
class RequestValidationTest extends IntegrationTest
{
	@Test
	void creatingABusinessCannotSmuggleInAnApprovedStatus() throws Exception
	{
		// CreateBusinessRequest has no "status" field; the extra key is ignored, not bound.
		// When bodies were bound straight onto Business, its setStatus(...) was a setter Jackson would
		// happily call here.
		mvc.perform(post("/api/v1/businesses").session(loginAs(Fixture.SUPER_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Self-Approved Salon", "description": "x", "image": "x", "status": "APPROVED"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING"));
	}

	/** Validation messages named fields in Java, such as firstName and addresses[0].postalCode. */
	@Test
	void validationMessagesNameFieldsAsTheClientSentThem() throws Exception
	{
		mvc.perform(post("/api/v1/users").session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name": "", "last_name": "Hire", "role": "EMPLOYEE", "email": "newhire@glow.test",
								 "password": "Password123!", "addresses": [{"street": "1 New Street", "city": "",
								 "country": "Germany", "postal_code": "%s", "latitude": 52.5}]}
								""".formatted("1".repeat(21))))
				.andExpect(status().isBadRequest())
				// A rule over several fields, such as the coordinate pair, is named by where it is.
				.andExpect(jsonPath("$.message").value("addresses[0] latitude and longitude must be given together; "
						+ "addresses[0].city must not be blank; addresses[0].postal_code size must be between 0 and 20; "
						+ "first_name must not be blank"));
	}

	@Test
	void creatingABusinessRejectsABlankName() throws Exception
	{
		mvc.perform(post("/api/v1/businesses").session(loginAs(Fixture.SUPER_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "  ", "description": "x", "image": "x"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void creatingAUserRejectsAPasswordShorterThanEightCharacters() throws Exception
	{
		mvc.perform(post("/api/v1/users").session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name":"New","last_name":"Hire","email":"short-pw@glow.test","password":"short1","role":"EMPLOYEE"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void creatingAUserRejectsAPasswordOverSeventyTwoBytes() throws Exception
	{
		// 40 characters, but 80 bytes in UTF-8: past BCrypt's limit.
		mvc.perform(post("/api/v1/users").session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name":"New","last_name":"Hire","email":"long-pw@glow.test","password":"%s","role":"EMPLOYEE"}
								""".formatted("ä".repeat(40))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
						"password must be at most 72 bytes in UTF-8, where a letter outside English takes 2 or more"));
	}

	@Test
	void creatingAUserRejectsAMalformedEmail() throws Exception
	{
		mvc.perform(post("/api/v1/users").session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name":"New","last_name":"Hire","email":"not-an-email","password":"Password123!","role":"EMPLOYEE"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void creatingAServiceRejectsAZeroDuration() throws Exception
	{
		mvc.perform(post("/api/v1/businesses/" + Fixture.GLOW + "/services").session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Instant Cut", "description": "x", "duration_minutes": 0, "price": 10.00, "is_active": true}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void creatingAServiceRejectsANegativePrice() throws Exception
	{
		mvc.perform(post("/api/v1/businesses/" + Fixture.GLOW + "/services").session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Bargain Cut", "description": "x", "duration_minutes": 30, "price": -5.00, "is_active": true}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void updatingAnAddressRejectsABlankStreet() throws Exception
	{
		mvc.perform(put("/api/v1/addresses/" + Fixture.GLOW_ADDRESS).session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"street": "", "city": "Berlin", "country": "Germany"}
								"""))
				.andExpect(status().isBadRequest());
	}
}
