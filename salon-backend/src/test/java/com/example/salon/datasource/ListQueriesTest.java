package com.example.salon.datasource;

import com.example.salon.support.IntegrationTest;
import com.example.salon.support.SqlStatements;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A list's addresses, contacts and services are read in one query each, not one per row (BE-15): the
 * number of queries doesn't grow with the number of rows.
 */
@ExtendWith(OutputCaptureExtension.class)
class ListQueriesTest extends IntegrationTest
{
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void listingTenSalonsTakesNoMoreQueriesThanListingThree(CapturedOutput output) throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);
		int forThree = SqlStatements.countDuring(output, () ->
				mvc.perform(get("/api/v1/businesses").session(superAdmin)).andExpect(status().isOk()));

		for (int i = 1; i <= 7; i++) {
			Long id = jdbcTemplate.queryForObject("""
					INSERT INTO businesses (name, description, image, status)
					VALUES (?, 'More salon.', 'https://example.com/more.png', 'APPROVED') RETURNING id
					""", Long.class, "Salon " + i);
			jdbcTemplate.update("INSERT INTO addresses (street, city, country, business_id) VALUES ('1 Main St.', 'Berlin', 'Germany', ?)", id);
			jdbcTemplate.update("INSERT INTO contacts (type, value, business_id) VALUES ('phone', '+49 30 1000000', ?)", id);
			jdbcTemplate.update("INSERT INTO services (business_id, name, duration_minutes, price, is_active) VALUES (?, 'Cut', 30, 20.00, true)", id);
		}
		int forTen = SqlStatements.countDuring(output, () ->
				mvc.perform(get("/api/v1/businesses").session(superAdmin)).andExpect(status().isOk()));

		assertThat(forTen).isEqualTo(forThree);
	}

	@Test
	void listingThirteenUsersTakesNoMoreQueriesThanListingSix(CapturedOutput output) throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);
		int forSix = SqlStatements.countDuring(output, () ->
				mvc.perform(get("/api/v1/users").session(superAdmin)).andExpect(status().isOk()));

		for (int i = 1; i <= 7; i++) {
			Long id = jdbcTemplate.queryForObject("""
					INSERT INTO users (first_name, last_name, role) VALUES ('More', ?, 'CUSTOMER') RETURNING id
					""", Long.class, "Customer " + i);
			jdbcTemplate.update("INSERT INTO addresses (street, city, country, user_id) VALUES ('2 Side St.', 'Berlin', 'Germany', ?)", id);
			jdbcTemplate.update("INSERT INTO contacts (type, value, user_id) VALUES ('phone', '+49 30 2000000', ?)", id);
		}
		int forThirteen = SqlStatements.countDuring(output, () ->
				mvc.perform(get("/api/v1/users").session(superAdmin)).andExpect(status().isOk()));

		assertThat(forThirteen).isEqualTo(forSix);
	}
}
