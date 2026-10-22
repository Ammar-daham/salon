package com.example.salon.controller;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A removed service, staff member or customer is gone from the API but stays in the database. */
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

		mvc.perform(delete(haircut).session(admin)).andExpect(status().isNoContent());

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
		// Nor is she still down for it underneath.
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM staff_services WHERE service_id = ?",
				Integer.class, Fixture.GLOW_HAIRCUT)).isZero();
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

		mvc.perform(delete(mia).session(admin)).andExpect(status().isNoContent());

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

	/**
	 * A removed staff member is out of the API's reach, and comes back as a new staff record, so their shifts,
	 * services and time off go with them. Their appointments stay: they're the salon's history.
	 */
	@Test
	void removingAStaffMemberClearsTheirShiftsServicesAndTimeOff() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		jdbcTemplate.update("""
				INSERT INTO appointments (business_id, customer_id, staff_id, service_id, starts_at, ends_at, price, status)
				VALUES (?, ?, ?, ?, '2025-03-04 10:00 Europe/Berlin', '2025-03-04 10:45 Europe/Berlin', 30, 'COMPLETED')
				""", Fixture.GLOW, Fixture.GLOW_CUSTOMER, Fixture.GLOW_STAFF, Fixture.GLOW_HAIRCUT);
		assertThat(rowsOf(Fixture.GLOW_STAFF)).containsExactly(2, 1, 1, 1);

		mvc.perform(delete(GLOW + "/staff/" + Fixture.GLOW_STAFF).session(admin)).andExpect(status().isNoContent());

		// Shifts, services, time off, appointments.
		assertThat(rowsOf(Fixture.GLOW_STAFF)).containsExactly(0, 0, 0, 1);
		assertThat(isDeleted("staff", Fixture.GLOW_STAFF)).isTrue();
	}

	@Test
	void aRemovedCustomerIsGoneFromTheListButKeptInTheDatabase() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		String olivia = GLOW + "/customers/" + Fixture.GLOW_CUSTOMER;

		mvc.perform(delete(olivia).session(admin)).andExpect(status().isNoContent());

		mvc.perform(get(GLOW + "/customers").session(admin))
				.andExpect(jsonPath("$.items", hasSize(0)))
				.andExpect(jsonPath("$.total_items").value(0));
		mvc.perform(put(olivia).session(admin).contentType(MediaType.APPLICATION_JSON).content("""
				{"first_name": "Olivia", "last_name": "Client"}
				""")).andExpect(status().isNotFound());
		mvc.perform(delete(olivia).session(admin)).andExpect(status().isNotFound());
		assertThat(isDeleted("customers", Fixture.GLOW_CUSTOMER)).isTrue();
	}

	/** How many shifts, services, time off and appointments the staff member has in the database. */
	private List<Integer> rowsOf(long staffId)
	{
		return Stream.of("staff_schedules", "staff_services", "staff_time_off", "appointments")
				.map(table -> jdbcTemplate.queryForObject("SELECT count(*) FROM " + table + " WHERE staff_id = ?",
						Integer.class, staffId))
				.toList();
	}

	private boolean isDeleted(String table, long id)
	{
		return jdbcTemplate.queryForObject("SELECT deleted_at IS NOT NULL FROM " + table + " WHERE id = ?",
				Boolean.class, id);
	}
}
