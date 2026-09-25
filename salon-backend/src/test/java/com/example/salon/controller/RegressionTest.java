package com.example.salon.controller;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * One test per bug that has already shipped once, so none of them can ship again.
 * The commit that fixed each bug is named in the test.
 */
class RegressionTest extends IntegrationTest
{
	/** 0ef369c: every address read swapped street and country. */
	@Test
	void addressReadsKeepStreetAndCountryInTheRightFields() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		mvc.perform(get("/api/v1/addresses/" + Fixture.GLOW_ADDRESS).session(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.street").value("12 Rosenthaler Str."))
				.andExpect(jsonPath("$.country").value("Germany"))
				.andExpect(jsonPath("$.postal_code").value("10119"));

		mvc.perform(get("/api/v1/addresses").session(admin))
				.andExpect(jsonPath("$[?(@.id == 1)].street").value("12 Rosenthaler Str."))
				.andExpect(jsonPath("$[?(@.id == 1)].country").value("Germany"));

		mvc.perform(get("/api/v1/businesses/" + Fixture.GLOW).session(admin))
				.andExpect(jsonPath("$.addresses[0].street").value("12 Rosenthaler Str."))
				.andExpect(jsonPath("$.addresses[0].country").value("Germany"));
	}

	/** 0ef369c: reading a never-updated address (updated_at NULL) crashed with an NPE. */
	@Test
	void neverUpdatedAddressCanBeReadAndUpdateSetsUpdatedAt() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		mvc.perform(get("/api/v1/addresses/" + Fixture.GLOW_ADDRESS).session(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.updated_at").value(nullValue()));

		mvc.perform(put("/api/v1/addresses/" + Fixture.GLOW_ADDRESS).session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"street": "99 New Street", "city": "Berlin", "country": "Germany", "postal_code": "10119"}
								"""))
				.andExpect(status().isOk());

		mvc.perform(get("/api/v1/addresses/" + Fixture.GLOW_ADDRESS).session(admin))
				.andExpect(jsonPath("$.street").value("99 New Street"))
				.andExpect(jsonPath("$.country").value("Germany"))
				.andExpect(jsonPath("$.updated_at").value(notNullValue()));
	}

	/** b1403ef: a trailing comma in the SQL made every GET /contacts/{id} a BadSqlGrammarException. */
	@Test
	void contactCanBeReadById() throws Exception
	{
		mvc.perform(get("/api/v1/contacts/" + Fixture.GLOW_CONTACT).session(loginAs(Fixture.GLOW_ADMIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.type").value("phone"))
				.andExpect(jsonPath("$.value").value("+49 30 1234501"))
				.andExpect(jsonPath("$.updated_at").value(nullValue()));
	}

	/** 83a3062: the DELETE route bound a path variable that didn't exist, so every delete failed. */
	@Test
	void deletingAServiceRemovesItFromTheBusiness() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		mvc.perform(delete("/api/v1/businesses/" + Fixture.GLOW + "/services/" + Fixture.GLOW_MANICURE).session(admin))
				.andExpect(status().isOk());

		mvc.perform(get("/api/v1/businesses/" + Fixture.GLOW).session(admin))
				.andExpect(jsonPath("$.services", hasSize(1)))
				.andExpect(jsonPath("$.services[0].id").value(Fixture.GLOW_HAIRCUT));
	}

	/** updateBusinessById COALESCE: omitting status/image used to reset status and violate image NOT NULL. */
	@Test
	void updatingABusinessWithoutImageOrStatusKeepsBoth() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		mvc.perform(put("/api/v1/businesses/" + Fixture.GLOW).session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Glow Beauty Studio", "description": "Now with brows."}
								"""))
				.andExpect(status().isOk());

		mvc.perform(get("/api/v1/businesses/" + Fixture.GLOW).session(admin))
				.andExpect(jsonPath("$.description").value("Now with brows."))
				.andExpect(jsonPath("$.status").value("APPROVED"))
				.andExpect(jsonPath("$.image").value("https://example.com/glow.png"))
				.andExpect(jsonPath("$.updated_at").value(notNullValue()));
	}

	/** Pins the snake_case wire contract the admin panel's mappers depend on. */
	@Test
	void serviceRoundTripsInSnakeCase() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		mvc.perform(post("/api/v1/businesses/" + Fixture.GLOW + "/services").session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Blow Dry", "description": "Wash and blow dry.", "duration_minutes": 25, "price": 20.00, "is_active": false}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(notNullValue()));

		mvc.perform(get("/api/v1/businesses/" + Fixture.GLOW).session(admin))
				.andExpect(jsonPath("$.created_at").value(notNullValue()))
				.andExpect(jsonPath("$.services", hasSize(3)))
				.andExpect(jsonPath("$.services[?(@.name == 'Blow Dry')].duration_minutes").value(25))
				.andExpect(jsonPath("$.services[?(@.name == 'Blow Dry')].is_active").value(false));
	}

	/** BE-10: deleting a business takes no body and removes its real children - no orphaned rows. */
	@Test
	void deletingABusinessWithoutABodyDeletesItsChildren() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);

		mvc.perform(delete("/api/v1/businesses/" + Fixture.GLOW).session(superAdmin))
				.andExpect(status().isOk());

		mvc.perform(get("/api/v1/contacts/" + Fixture.GLOW_CONTACT).session(superAdmin))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/addresses/" + Fixture.GLOW_ADDRESS).session(superAdmin))
				.andExpect(status().isNotFound());
	}

	/** BE-10: the deleted business's contact value is freed, not burned forever by the global UNIQUE. */
	@Test
	void deletingABusinessFreesItsContactValueForReuse() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);

		mvc.perform(delete("/api/v1/businesses/" + Fixture.GLOW).session(superAdmin))
				.andExpect(status().isOk());

		// The phone number that belonged to the deleted business can be registered again.
		mvc.perform(post("/api/v1/users").session(superAdmin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name":"New","last_name":"Hire","email":"newhire@urban.test","password":"Password123!","role":"EMPLOYEE","business_id":%d,"contacts":[{"type":"phone","value":"+49 30 1234501"}]}
								""".formatted(Fixture.URBAN)))
				.andExpect(status().isOk());
	}

	/** BE-10: deleting a user takes no body and removes the user's own children. */
	@Test
	void deletingAUserWithoutABodyDeletesTheirChildren() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);

		mvc.perform(delete("/api/v1/users/" + Fixture.GLOW_EMPLOYEE_ID).session(superAdmin))
				.andExpect(status().isOk());

		mvc.perform(get("/api/v1/contacts/" + Fixture.GLOW_EMPLOYEE_PERSONAL_CONTACT).session(superAdmin))
				.andExpect(status().isNotFound());
	}

	/** BE-09: a duplicate-key insert must surface as a conflict, not a swallowed false success. */
	@Test
	void creatingABusinessWithADuplicateNameIsAConflict() throws Exception
	{
		mvc.perform(post("/api/v1/businesses").session(loginAs(Fixture.SUPER_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Glow Beauty Studio", "description": "dup", "image": "https://example.com/x.png"}
								"""))
				.andExpect(status().isConflict());
	}

	/** BE-18: errors carry the stable ErrorCode.code on the wire, not an ad-hoc string. */
	@Test
	void notFoundErrorsCarryTheStableNotFoundCode() throws Exception
	{
		mvc.perform(get("/api/v1/businesses/999999").session(loginAs(Fixture.SUPER_ADMIN)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
	}

	/** BE-27: a server-side NPE (here, an update body with no role) is a logged 500, no longer a masked 400. */
	@Test
	void aServerSideNullPointerBecomesA500NotAMasked400() throws Exception
	/** BE-07: a nested new child (id null) on a business update used to auto-unbox to an NPE / 400. */
	@Test
	void updatingABusinessWithANewNestedChildNoLongerCrashes() throws Exception
	{
		mvc.perform(put("/api/v1/businesses/" + Fixture.GLOW).session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Glow Beauty Studio", "contacts": [{"type": "email", "value": "brand-new@glow.test"}]}
								"""))
				.andExpect(status().isOk());
	}

	/** BE-07: same for a user update with a nested new child. */
	@Test
	void updatingAUserWithANewNestedChildNoLongerCrashes() throws Exception
	{
		mvc.perform(put("/api/v1/users/" + Fixture.GLOW_EMPLOYEE_ID).session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name": "Mia", "last_name": "Stylist"}
								"""))
				.andExpect(status().isInternalServerError());
								{"first_name": "Mia", "last_name": "Stylist", "role": "EMPLOYEE", "contacts": [{"type": "phone", "value": "+49 30 0000000"}]}
								"""))
				.andExpect(status().isOk());
	}
}
