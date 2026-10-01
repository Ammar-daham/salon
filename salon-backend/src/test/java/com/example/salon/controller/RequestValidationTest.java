package com.example.salon.controller;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BE-23: request bodies are now bound onto per-endpoint DTOs with Bean Validation, instead of
 * straight onto the domain models. One test per rule that DTO is responsible for.
 */
class RequestValidationTest extends IntegrationTest
{
	@Test
	void creatingABusinessCannotSmuggleInAnApprovedStatus() throws Exception
	{
		// CreateBusinessRequest has no "status" field; the extra key is ignored, not bound.
		// Before BE-23, Business.setStatus(...) had a real setter Jackson would happily call here.
		mvc.perform(post("/api/v1/businesses").session(loginAs(Fixture.SUPER_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Self-Approved Salon", "description": "x", "image": "x", "status": "APPROVED"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING"));
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
