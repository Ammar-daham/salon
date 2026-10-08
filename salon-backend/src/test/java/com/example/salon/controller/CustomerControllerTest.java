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

/** DB-03: a salon's customers are their own records, scoped to that salon. */
class CustomerControllerTest extends IntegrationTest
{
	private static final String NEW_CUSTOMER = """
			{"first_name": "Ella", "last_name": "Walk-in", "email": "ella@example.test", "phone": "+49 30 7770009",
			 "notes": "Allergic to latex.", "marketing_consent": true}
			""";

	private static String customers(long businessId)
	{
		return "/api/v1/businesses/" + businessId + "/customers";
	}

	@Test
	void adminCanAddAndReadACustomer() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		String location = mvc.perform(post(customers(Fixture.GLOW)).session(admin)
						.contentType(MediaType.APPLICATION_JSON).content(NEW_CUSTOMER))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(notNullValue()))
				.andExpect(jsonPath("$.first_name").value("Ella"))
				.andExpect(jsonPath("$.marketing_consent").value(true))
				.andExpect(jsonPath("$.created_at").value(notNullValue()))
				// Whose client it is, for lists across salons.
				.andExpect(jsonPath("$.business_id").value(Fixture.GLOW))
				.andExpect(jsonPath("$.business_name").value("Glow Beauty Studio"))
				.andReturn().getResponse().getHeader("Location");

		mvc.perform(get(location).session(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.notes").value("Allergic to latex."));

		mvc.perform(get(customers(Fixture.GLOW)).session(admin))
				.andExpect(jsonPath("$.items", hasSize(2)));
	}

	@Test
	void employeesCanListAndAddCustomersOfTheirOwnSalon() throws Exception
	{
		MockHttpSession employee = loginAs(Fixture.GLOW_EMPLOYEE);

		mvc.perform(get(customers(Fixture.GLOW)).session(employee))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].first_name").value("Olivia"));

		mvc.perform(post(customers(Fixture.GLOW)).session(employee)
						.contentType(MediaType.APPLICATION_JSON).content(NEW_CUSTOMER))
				.andExpect(status().isCreated());
	}

	@Test
	void employeesCannotEditOrDeleteCustomers() throws Exception
	{
		MockHttpSession employee = loginAs(Fixture.GLOW_EMPLOYEE);

		mvc.perform(put(customers(Fixture.GLOW) + "/" + Fixture.GLOW_CUSTOMER).session(employee)
						.contentType(MediaType.APPLICATION_JSON).content(NEW_CUSTOMER))
				.andExpect(status().isForbidden());
		mvc.perform(delete(customers(Fixture.GLOW) + "/" + Fixture.GLOW_CUSTOMER).session(employee))
				.andExpect(status().isForbidden());
	}

	@Test
	void anotherSalonsCustomersAreOffLimits() throws Exception
	{
		MockHttpSession urbanAdmin = loginAs(Fixture.URBAN_ADMIN);

		mvc.perform(get(customers(Fixture.GLOW)).session(urbanAdmin)).andExpect(status().isForbidden());
		mvc.perform(get(customers(Fixture.GLOW) + "/" + Fixture.GLOW_CUSTOMER).session(urbanAdmin))
				.andExpect(status().isForbidden());
		mvc.perform(post(customers(Fixture.GLOW)).session(urbanAdmin)
						.contentType(MediaType.APPLICATION_JSON).content(NEW_CUSTOMER))
				.andExpect(status().isForbidden());
		mvc.perform(get(customers(Fixture.GLOW)).session(loginAs(Fixture.URBAN_EMPLOYEE)))
				.andExpect(status().isForbidden());
	}

	@Test
	void aCustomerIdFromAnotherSalonIsNotFoundThroughYourOwnPath() throws Exception
	{
		MockHttpSession glowAdmin = loginAs(Fixture.GLOW_ADMIN);
		String foreign = customers(Fixture.GLOW) + "/" + Fixture.URBAN_CUSTOMER;

		mvc.perform(get(foreign).session(glowAdmin)).andExpect(status().isNotFound());
		mvc.perform(put(foreign).session(glowAdmin)
						.contentType(MediaType.APPLICATION_JSON).content(NEW_CUSTOMER))
				.andExpect(status().isNotFound());
		mvc.perform(delete(foreign).session(glowAdmin)).andExpect(status().isNotFound());

		mvc.perform(get(customers(Fixture.URBAN) + "/" + Fixture.URBAN_CUSTOMER).session(loginAs(Fixture.URBAN_ADMIN)))
				.andExpect(jsonPath("$.first_name").value("Noah"));
	}

	@Test
	void adminCanReplaceACustomer() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		String olivia = customers(Fixture.GLOW) + "/" + Fixture.GLOW_CUSTOMER;

		mvc.perform(get(olivia).session(admin)).andExpect(jsonPath("$.updated_at").value(nullValue()));

		mvc.perform(put(olivia).session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name": "Olivia", "last_name": "Client-Smith", "phone": "+49 30 7770001", "marketing_consent": false}
								"""))
				.andExpect(status().isOk());

		// PUT replaces the record: omitted email and notes are cleared.
		mvc.perform(get(olivia).session(admin))
				.andExpect(jsonPath("$.last_name").value("Client-Smith"))
				.andExpect(jsonPath("$.email").value(nullValue()))
				.andExpect(jsonPath("$.notes").value(nullValue()))
				.andExpect(jsonPath("$.marketing_consent").value(false))
				.andExpect(jsonPath("$.updated_at").value(notNullValue()));
	}

	@Test
	void adminCanDeleteACustomer() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		String olivia = customers(Fixture.GLOW) + "/" + Fixture.GLOW_CUSTOMER;

		mvc.perform(delete(olivia).session(admin)).andExpect(status().isOk());
		mvc.perform(get(olivia).session(admin)).andExpect(status().isNotFound());
	}

	@Test
	void leavingOutMarketingConsentMeansNoConsent() throws Exception
	{
		// Was a 400 "Malformed JSON request body": Jackson 3 rejects a missing primitive boolean.
		mvc.perform(post(customers(Fixture.GLOW)).session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name": "Ella", "last_name": "Walk-in"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.marketing_consent").value(false));
	}

	@Test
	void customersAreValidated() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		mvc.perform(post(customers(Fixture.GLOW)).session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name": " ", "last_name": "Walk-in"}
								"""))
				.andExpect(status().isBadRequest());
		mvc.perform(post(customers(Fixture.GLOW)).session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name": "Ella", "last_name": "Walk-in", "email": "not-an-email"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void superAdminGetsNotFoundForASalonThatDoesNotExist() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);

		mvc.perform(get(customers(999)).session(superAdmin)).andExpect(status().isNotFound());
		mvc.perform(post(customers(999)).session(superAdmin)
						.contentType(MediaType.APPLICATION_JSON).content(NEW_CUSTOMER))
				.andExpect(status().isNotFound());
	}

	@Test
	void anonymousRequestsAreRejected() throws Exception
	{
		mvc.perform(get(customers(Fixture.GLOW))).andExpect(status().isUnauthorized());
		mvc.perform(post(customers(Fixture.GLOW))
						.contentType(MediaType.APPLICATION_JSON).content(NEW_CUSTOMER))
				.andExpect(status().isUnauthorized());
	}
}
