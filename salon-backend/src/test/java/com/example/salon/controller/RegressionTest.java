package com.example.salon.controller;

import com.example.salon.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
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
				.andExpect(status().isNoContent());

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
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(notNullValue()));

		mvc.perform(get("/api/v1/businesses/" + Fixture.GLOW).session(admin))
				.andExpect(jsonPath("$.created_at").value(notNullValue()))
				.andExpect(jsonPath("$.services", hasSize(3)))
				.andExpect(jsonPath("$.services[?(@.name == 'Blow Dry')].duration_minutes").value(25))
				.andExpect(jsonPath("$.services[?(@.name == 'Blow Dry')].is_active").value(false));
	}

	/**
	 * Every create used to build a ResponseEntity.created(location) and then return only its body,
	 * which dropped the status and the Location header, so it answered 200 with no way to find the
	 * new resource but its body. Each create now answers 201 with a Location that resolves.
	 */
	@Test
	void creatingAResourceReturns201WithALocationHeaderThatResolves() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);

		String businessLocation = mvc.perform(post("/api/v1/businesses").session(superAdmin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Location Spa", "description": "x", "image": "x"}
								"""))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");
		mvc.perform(get(businessLocation).session(superAdmin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Location Spa"));

		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		String userLocation = mvc.perform(post("/api/v1/users").session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name":"Be13","last_name":"Check","email":"be13check@glow.test","password":"Password123!","role":"EMPLOYEE"}
								"""))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");
		mvc.perform(get(userLocation).session(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.first_name").value("Be13"));

		String serviceLocation = mvc.perform(post("/api/v1/businesses/" + Fixture.GLOW + "/services").session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Location Trim", "description": "x", "duration_minutes": 15, "price": 10.00, "is_active": true}
								"""))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");
		mvc.perform(get(serviceLocation).session(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Location Trim"));
	}

	/** Deleting a business takes no body and removes its real children - no orphaned rows. */
	@Test
	void deletingABusinessWithoutABodyDeletesItsChildren() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);
		deleteGlowStaff(superAdmin);

		mvc.perform(delete("/api/v1/businesses/" + Fixture.GLOW).session(superAdmin))
				.andExpect(status().isNoContent());

		mvc.perform(get("/api/v1/contacts/" + Fixture.GLOW_CONTACT).session(superAdmin))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/addresses/" + Fixture.GLOW_ADDRESS).session(superAdmin))
				.andExpect(status().isNotFound());
	}

	/** Services belong to one business now, so deleting it cascades to its services
	 *  instead of leaving them behind in what used to be modeled as a shared table. */
	@Test
	void deletingABusinessRemovesItsServices() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);
		deleteGlowStaff(superAdmin);

		mvc.perform(delete("/api/v1/businesses/" + Fixture.GLOW).session(superAdmin))
				.andExpect(status().isNoContent());

		mvc.perform(get("/api/v1/businesses/" + Fixture.GLOW + "/services/" + Fixture.GLOW_HAIRCUT).session(superAdmin))
				.andExpect(status().isNotFound());
	}

	/** The deleted business's contact value is freed, not burned forever by the global UNIQUE. */
	@Test
	void deletingABusinessFreesItsContactValueForReuse() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);
		deleteGlowStaff(superAdmin);

		mvc.perform(delete("/api/v1/businesses/" + Fixture.GLOW).session(superAdmin))
				.andExpect(status().isNoContent());

		// The phone number that belonged to the deleted business can be registered again.
		mvc.perform(post("/api/v1/users").session(superAdmin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name":"New","last_name":"Hire","email":"newhire@urban.test","password":"Password123!","role":"EMPLOYEE","business_id":%d,"contacts":[{"type":"phone","value":"+49 30 1234501"}]}
								""".formatted(Fixture.URBAN)))
				.andExpect(status().isCreated());
	}

	/** Deleting a user takes no body and removes the user's own children. */
	@Test
	void deletingAUserWithoutABodyDeletesTheirChildren() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);

		mvc.perform(delete("/api/v1/users/" + Fixture.GLOW_EMPLOYEE_ID).session(superAdmin))
				.andExpect(status().isNoContent());

		mvc.perform(get("/api/v1/contacts/" + Fixture.GLOW_EMPLOYEE_PERSONAL_CONTACT).session(superAdmin))
				.andExpect(status().isNotFound());
	}

	/** A duplicate-key insert must surface as a conflict, not a swallowed false success. */
	@Test
	void creatingABusinessWithADuplicateNameIsAConflict() throws Exception
	{
		mvc.perform(post("/api/v1/businesses").session(loginAs(Fixture.SUPER_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Glow Beauty Studio", "description": "dup", "image": "https://example.com/x.png"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("CONFLICT"))
				.andExpect(jsonPath("$.message").value("Business with name Glow Beauty Studio already exists."));
	}

	/** Changing a user's contact to a value they already have was a 400, as POST /users answers 409. */
	@Test
	void changingAUsersContactToOneTheyHaveIsAConflict() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		String created = mvc.perform(post("/api/v1/users").session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name":"New","last_name":"Hire","email":"newhire@glow.test","password":"Password123!","role":"EMPLOYEE","contacts":[{"type":"phone","value":"+49 30 5550001"},{"type":"phone","value":"+49 30 5550002"}]}
								"""))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		long second = ((Number) JsonPath.read(created, "$.contacts[1].id")).longValue();

		mvc.perform(put("/api/v1/contacts/" + second).session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"type": "phone", "value": "+49 30 5550001"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("CONFLICT"))
				.andExpect(jsonPath("$.message").value("Contact already exists."));
	}

	/** Same for a salon's contact, as POST /businesses answers 409. */
	@Test
	void changingASalonsContactToOneItHasIsAConflict() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);
		String created = mvc.perform(post("/api/v1/businesses").session(superAdmin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Two Phones Salon", "description": "x", "image": "https://example.com/x.png", "contacts": [{"type": "phone", "value": "+49 30 5550001"}, {"type": "phone", "value": "+49 30 5550002"}]}
								"""))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		long second = ((Number) JsonPath.read(created, "$.contacts[1].id")).longValue();

		mvc.perform(put("/api/v1/contacts/" + second).session(superAdmin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"type": "phone", "value": "+49 30 5550001"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("CONFLICT"))
				.andExpect(jsonPath("$.message").value("Contact already exists."));
	}

	/** GET /customers?business_id= for a salon that doesn't exist was a 200 with an empty page, not a 404. */
	@Test
	void listingCustomersOfASalonThatDoesntExistIsNotFound() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);

		mvc.perform(get("/api/v1/customers?business_id=9999").session(superAdmin))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Business with id 9999 not found"));

		mvc.perform(get("/api/v1/businesses/9999/customers").session(superAdmin))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Business with id 9999 not found"));
	}

	/** Adding a service to a salon that doesn't exist was a 400, from the foreign key, not a 404. */
	@Test
	void addingAServiceToASalonThatDoesntExistIsNotFound() throws Exception
	{
		mvc.perform(post("/api/v1/businesses/9999/services").session(loginAs(Fixture.SUPER_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Blow Dry", "description": "Wash and blow dry.", "duration_minutes": 25, "price": 20.00, "is_active": true}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Business with id 9999 not found"));
	}

	/** Renaming a salon to a taken name was a 400, from the uncaught DuplicateKeyException, not a 409. */
	@Test
	void renamingABusinessToATakenNameIsAConflict() throws Exception
	{
		mvc.perform(put("/api/v1/businesses/" + Fixture.GLOW).session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Urban Cuts Barbershop"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value("CONFLICT"))
				.andExpect(jsonPath("$.message").value("Business with name Urban Cuts Barbershop already exists."));
	}

	/** Errors carry the stable ErrorCode.code on the wire, not an ad-hoc string. */
	@Test
	void notFoundErrorsCarryTheStableNotFoundCode() throws Exception
	{
		mvc.perform(get("/api/v1/businesses/999999").session(loginAs(Fixture.SUPER_ADMIN)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
	}

	/** A path variable that isn't a number fell through to the catch-all and came back as a 500. */
	@Test
	void aMalformedPathVariableIsARejectedRequestNotAServerError() throws Exception
	{
		mvc.perform(get("/api/v1/businesses/glow").session(loginAs(Fixture.SUPER_ADMIN)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("id has an invalid value"));
	}

	/**
	 * A missing required field is a validation 400, not a server crash. An update body without
	 * "role" used to unbox a null Role into a NullPointerException, which a handler then passed off
	 * as a 400. UpdateUserRequest now rejects that body before it reaches that code, and any truly
	 * unexpected exception is a logged 500, from the catch-all in GlobalControllerExceptionHandler.
	 */
	@Test
	void aMissingRequiredFieldIsARejectedRequestNotAServerCrash() throws Exception
	{
		mvc.perform(put("/api/v1/users/" + Fixture.GLOW_EMPLOYEE_ID).session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name": "Mia", "last_name": "Stylist"}
								"""))
				.andExpect(status().isBadRequest());
	}
	
	/** A nested new child (id null) on a business update used to auto-unbox to an NPE / 400. */
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

	/** Same for a user update with a nested new child. */
	@Test
	void updatingAUserWithANewNestedChildNoLongerCrashes() throws Exception
	{
		mvc.perform(put("/api/v1/users/" + Fixture.GLOW_EMPLOYEE_ID).session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name": "Mia", "last_name": "Stylist", "role": "EMPLOYEE", "contacts": [{"type": "phone", "value": "+49 30 0000000"}]}
								"""))
				.andExpect(status().isOk());
	}

	/** A user update never set users.updated_at. */
	@Test
	void updatingAUserSetsUpdatedAt() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		mvc.perform(get("/api/v1/users/" + Fixture.GLOW_EMPLOYEE_ID).session(admin))
				.andExpect(jsonPath("$.updated_at").value(nullValue()));

		mvc.perform(put("/api/v1/users/" + Fixture.GLOW_EMPLOYEE_ID).session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name": "Mia", "last_name": "Senior-Stylist", "role": "EMPLOYEE"}
								"""))
				.andExpect(status().isOk());

		mvc.perform(get("/api/v1/users/" + Fixture.GLOW_EMPLOYEE_ID).session(admin))
				.andExpect(jsonPath("$.last_name").value("Senior-Stylist"))
				.andExpect(jsonPath("$.updated_at").value(notNullValue()));
	}

	/** A salon can only be deleted once it has no staff. */
	private void deleteGlowStaff(MockHttpSession superAdmin) throws Exception
	{
		for (long userId : new long[] {Fixture.GLOW_ADMIN_ID, Fixture.GLOW_EMPLOYEE_ID}) {
			mvc.perform(delete("/api/v1/users/" + userId).session(superAdmin))
					.andExpect(status().isNoContent());
		}
	}
}
