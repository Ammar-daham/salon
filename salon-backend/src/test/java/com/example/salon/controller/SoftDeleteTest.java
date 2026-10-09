package com.example.salon.controller;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** DB-13: a removed service, staff member or customer is gone from the API but stays in the database. */
class SoftDeleteTest extends IntegrationTest
{
	private static final String GLOW = "/api/v1/businesses/" + Fixture.GLOW;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void aRemovedServiceIsGoneFromEveryReadButKeptInTheDatabase() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		String haircut = GLOW + "/services/" + Fixture.GLOW_HAIRCUT;

		mvc.perform(delete(haircut).session(admin)).andExpect(status().isOk());

		mvc.perform(get(haircut).session(admin)).andExpect(status().isNotFound());
		mvc.perform(put(haircut).session(admin).contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "Back again", "duration_minutes": 45, "price": 45.00, "is_active": true}
				""")).andExpect(status().isNotFound());
		mvc.perform(delete(haircut).session(admin)).andExpect(status().isNotFound());
		mvc.perform(get(GLOW).session(admin))
				.andExpect(jsonPath("$.services", hasSize(1)))
				.andExpect(jsonPath("$.services[0].id").value(Fixture.GLOW_MANICURE));

		// Mia performed it; she doesn't any more, and it can't be given back to her.
		String miasServices = GLOW + "/staff/" + Fixture.GLOW_STAFF + "/services";
		mvc.perform(get(miasServices).session(admin)).andExpect(jsonPath("$", hasSize(0)));
		mvc.perform(put(miasServices).session(admin).contentType(MediaType.APPLICATION_JSON)
						.content("{\"service_ids\": [" + Fixture.GLOW_HAIRCUT + "]}"))
				.andExpect(status().isBadRequest());

		assertThat(isDeleted("services", Fixture.GLOW_HAIRCUT)).isTrue();
	}

	@Test
	void aRemovedStaffMemberIsKeptAndCanBeTakenOnAgain() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		String mia = GLOW + "/staff/" + Fixture.GLOW_STAFF;
		String hireMia = """
				{"user_id": %d, "title": "Senior Stylist", "is_active": true, "hired_at": "2027-01-04"}
				""".formatted(Fixture.GLOW_EMPLOYEE_ID);

		// One current staff record per person.
		mvc.perform(post(GLOW + "/staff").session(admin).contentType(MediaType.APPLICATION_JSON).content(hireMia))
				.andExpect(status().isConflict());

		mvc.perform(delete(mia).session(admin)).andExpect(status().isOk());

		mvc.perform(get(mia).session(admin)).andExpect(status().isNotFound());
		mvc.perform(get(mia + "/schedule").session(admin)).andExpect(status().isNotFound());
		mvc.perform(delete(mia).session(admin)).andExpect(status().isNotFound());
		mvc.perform(get(GLOW + "/staff").session(admin)).andExpect(jsonPath("$", hasSize(0)));
		assertThat(isDeleted("staff", Fixture.GLOW_STAFF)).isTrue();

		// Rehired later: a new record, the old one stays for the history.
		mvc.perform(post(GLOW + "/staff").session(admin).contentType(MediaType.APPLICATION_JSON).content(hireMia))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(not((int) Fixture.GLOW_STAFF)));
		mvc.perform(get(GLOW + "/staff").session(admin)).andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void aRemovedCustomerIsGoneFromTheListButKeptInTheDatabase() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		String olivia = GLOW + "/customers/" + Fixture.GLOW_CUSTOMER;

		mvc.perform(delete(olivia).session(admin)).andExpect(status().isOk());

		mvc.perform(get(GLOW + "/customers").session(admin))
				.andExpect(jsonPath("$.items", hasSize(0)))
				.andExpect(jsonPath("$.total_items").value(0));
		mvc.perform(put(olivia).session(admin).contentType(MediaType.APPLICATION_JSON).content("""
				{"first_name": "Olivia", "last_name": "Client"}
				""")).andExpect(status().isNotFound());
		mvc.perform(delete(olivia).session(admin)).andExpect(status().isNotFound());
		assertThat(isDeleted("customers", Fixture.GLOW_CUSTOMER)).isTrue();
	}

	private boolean isDeleted(String table, long id)
	{
		return jdbcTemplate.queryForObject("SELECT deleted_at IS NOT NULL FROM " + table + " WHERE id = ?",
				Boolean.class, id);
	}
}
