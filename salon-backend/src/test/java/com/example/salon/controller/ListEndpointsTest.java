package com.example.salon.controller;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The lists that grow come a page at a time, with their total, and are searched, filtered and sorted
 * in the database (BE-15).
 */
class ListEndpointsTest extends IntegrationTest
{
	private static final String BUSINESSES = "/api/v1/businesses";
	private static final String USERS = "/api/v1/users";
	private static final String CUSTOMERS = "/api/v1/customers";

	@Test
	void aListComesAPageAtATimeWithItsTotal() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);

		mvc.perform(get(BUSINESSES + "?size=2").session(superAdmin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].name", contains("Glow Beauty Studio", "Serenity Day Spa")))
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.total_items").value(3))
				.andExpect(jsonPath("$.total_pages").value(2));
		mvc.perform(get(BUSINESSES + "?size=2&page=2").session(superAdmin))
				.andExpect(jsonPath("$.items[*].name", contains("Urban Cuts Barbershop")));
		// Past the end: nothing on the page, but the totals still say how long the list is.
		mvc.perform(get(BUSINESSES + "?size=2&page=3").session(superAdmin))
				.andExpect(jsonPath("$.items", hasSize(0)))
				.andExpect(jsonPath("$.total_items").value(3));
		// Without page or size: the first 20.
		mvc.perform(get(USERS).session(superAdmin))
				.andExpect(jsonPath("$.items", hasSize(6)))
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.total_pages").value(1));
	}

	@Test
	void aPageStillCarriesEachRowsAddressesContactsAndServices() throws Exception
	{
		mvc.perform(get(BUSINESSES + "?q=glow").session(loginAs(Fixture.SUPER_ADMIN)))
				.andExpect(jsonPath("$.items[0].addresses[0].city").value("Berlin"))
				.andExpect(jsonPath("$.items[0].contacts[0].value").value("+49 30 1234501"))
				.andExpect(jsonPath("$.items[0].services", hasSize(2)));
		mvc.perform(get(USERS + "?q=mia").session(loginAs(Fixture.GLOW_ADMIN)))
				.andExpect(jsonPath("$.items[0].addresses[0].street").value("3 Private Lane"))
				.andExpect(jsonPath("$.items[0].contacts[0].value").value("+49 30 9990004"));
	}

	@Test
	void salonsAreSearchedByNameDescriptionAndCityAndFilteredByStatus() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);

		mvc.perform(get(BUSINESSES + "?q=URBAN").session(superAdmin))
				.andExpect(jsonPath("$.items[*].name", contains("Urban Cuts Barbershop")));
		mvc.perform(get(BUSINESSES + "?q=massage").session(superAdmin))
				.andExpect(jsonPath("$.items[*].name", contains("Serenity Day Spa")));
		mvc.perform(get(BUSINESSES + "?q=berlin").session(superAdmin))
				.andExpect(jsonPath("$.items[*].name", contains("Glow Beauty Studio")));
		// A wildcard in the search is just a character to look for.
		mvc.perform(get(BUSINESSES + "?q=%25").session(superAdmin))
				.andExpect(jsonPath("$.total_items").value(0));
		mvc.perform(get(BUSINESSES + "?status=PENDING").session(superAdmin))
				.andExpect(jsonPath("$.items[*].name", contains("Serenity Day Spa")))
				.andExpect(jsonPath("$.total_items").value(1));
	}

	@Test
	void aListIsSortedByWhatTheClientAsks() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);

		mvc.perform(get(BUSINESSES + "?sort=-name").session(superAdmin))
				.andExpect(jsonPath("$.items[*].name",
						contains("Urban Cuts Barbershop", "Serenity Day Spa", "Glow Beauty Studio")));
		mvc.perform(get(BUSINESSES + "?sort=status").session(superAdmin))
				.andExpect(jsonPath("$.items[*].status", contains("APPROVED", "APPROVED", "PENDING")));
		mvc.perform(get(USERS + "?role=ADMIN&sort=-email").session(superAdmin))
				.andExpect(jsonPath("$.items[*].email", contains(Fixture.URBAN_ADMIN, Fixture.GLOW_ADMIN)));
		mvc.perform(get(CUSTOMERS + "?sort=-business_name").session(superAdmin))
				.andExpect(jsonPath("$.items[*].business_name", contains("Urban Cuts Barbershop", "Glow Beauty Studio")));
	}

	@Test
	void usersAreSearchedByNameAndEmailWithinTheCallersBusiness() throws Exception
	{
		MockHttpSession glowAdmin = loginAs(Fixture.GLOW_ADMIN);

		mvc.perform(get(USERS).session(glowAdmin))
				.andExpect(jsonPath("$.total_items").value(2));
		mvc.perform(get(USERS + "?q=anna admin").session(glowAdmin))
				.andExpect(jsonPath("$.items[*].email", contains(Fixture.GLOW_ADMIN)));
		mvc.perform(get(USERS + "?q=stylist@glow").session(glowAdmin))
				.andExpect(jsonPath("$.items[*].email", contains(Fixture.GLOW_EMPLOYEE)));
		// Ben is an admin too, but of another salon.
		mvc.perform(get(USERS + "?q=admin").session(glowAdmin))
				.andExpect(jsonPath("$.items[*].email", contains(Fixture.GLOW_ADMIN)));
	}

	@Test
	void aSuperAdminListsCustomersAcrossSalonsOrOfOne() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);

		mvc.perform(get(CUSTOMERS).session(superAdmin))
				.andExpect(jsonPath("$.total_items").value(2))
				.andExpect(jsonPath("$.items[?(@.id == 2)].business_id").value((int) Fixture.URBAN))
				.andExpect(jsonPath("$.items[?(@.id == 2)].business_name").value("Urban Cuts Barbershop"));
		mvc.perform(get(CUSTOMERS + "?business_id=" + Fixture.URBAN).session(superAdmin))
				.andExpect(jsonPath("$.items[*].first_name", contains("Noah")));
		mvc.perform(get(CUSTOMERS + "?q=7770001").session(superAdmin))
				.andExpect(jsonPath("$.items[*].first_name", contains("Olivia")));
		mvc.perform(get(CUSTOMERS + "/" + Fixture.URBAN_CUSTOMER).session(superAdmin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.first_name").value("Noah"))
				.andExpect(jsonPath("$.business_name").value("Urban Cuts Barbershop"));
	}

	@Test
	void salonStaffOnlyEverSeeTheirOwnSalonsCustomers() throws Exception
	{
		for (String staff : new String[] {Fixture.GLOW_ADMIN, Fixture.GLOW_EMPLOYEE}) {
			MockHttpSession session = loginAs(staff);
			mvc.perform(get(CUSTOMERS).session(session))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.items[*].first_name", contains("Olivia")))
					.andExpect(jsonPath("$.total_items").value(1));
			mvc.perform(get(CUSTOMERS + "?business_id=" + Fixture.URBAN).session(session))
					.andExpect(status().isForbidden());
			mvc.perform(get(CUSTOMERS + "/" + Fixture.GLOW_CUSTOMER).session(session))
					.andExpect(status().isOk());
			// 404, not 403: an id doesn't reveal that another salon has that client.
			mvc.perform(get(CUSTOMERS + "/" + Fixture.URBAN_CUSTOMER).session(session))
					.andExpect(status().isNotFound());
		}
	}

	@Test
	void aBadPageSortOrFilterIsA400ThatSaysWhatsAllowed() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);

		mvc.perform(get(BUSINESSES + "?page=0").session(superAdmin))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("page must be 1 or more"));
		mvc.perform(get(BUSINESSES + "?size=101").session(superAdmin))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("size must be between 1 and 100"));
		mvc.perform(get(USERS + "?sort=password_hash").session(superAdmin))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("sort must be one of name, -name, email")));
		mvc.perform(get(BUSINESSES + "?status=OPEN").session(superAdmin))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("status has an invalid value"));
		mvc.perform(get(CUSTOMERS + "?page=two").session(superAdmin))
				.andExpect(status().isBadRequest());
	}
}
