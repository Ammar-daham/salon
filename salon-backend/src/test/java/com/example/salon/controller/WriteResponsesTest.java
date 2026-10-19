package com.example.salon.controller;

import com.example.salon.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every endpoint that changes a record answers the same way as the others of its kind: a POST answers
 * 201 with the record as stored and its address in {@code Location}, a PUT answers 200 with the record
 * as stored, and a DELETE answers 204 with no body.
 */
class WriteResponsesTest extends IntegrationTest
{
	private static final String GLOW = "/api/v1/businesses/" + Fixture.GLOW;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * Each answer has the {@code created_at} the database stored, and what the database fills in for a
	 * field the request didn't send. Booking an appointment is checked in AppointmentControllerTest.
	 */
	@Test
	void everyPostAnswersTheRecordAsStored() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);
		String mia = GLOW + "/staff/" + Fixture.GLOW_STAFF;

		create("/api/v1/businesses", "businesses", superAdmin, """
				{"name": "Fresh Cuts", "image": "https://example.com/fresh.png",
				 "addresses": [{"street": "1 New Street", "city": "Berlin", "country": "Germany"}]}
				""")
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.addresses[0].id").value(notNullValue()))
				.andExpect(jsonPath("$.contacts", hasSize(0)))
				.andExpect(jsonPath("$.services", hasSize(0)));
		create(GLOW + "/services", "services", superAdmin, """
				{"name": "Brow Shape", "duration_minutes": 15, "price": 12.00, "is_active": true}
				""")
				.andExpect(jsonPath("$.name").value("Brow Shape"))
				.andExpect(jsonPath("$.description").value(nullValue()));
		create("/api/v1/users", "users", superAdmin, """
				{"first_name": "New", "last_name": "Hire", "email": "newhire@glow.test", "password": "Password123!",
				 "role": "EMPLOYEE", "business_id": %d}
				""".formatted(Fixture.GLOW))
				.andExpect(jsonPath("$.email").value("newhire@glow.test"))
				.andExpect(jsonPath("$.addresses", hasSize(0)))
				.andExpect(jsonPath("$.contacts", hasSize(0)));
		create(GLOW + "/staff", "staff", superAdmin, """
				{"user_id": %d, "title": "Owner", "is_active": true, "hired_at": "2019-05-01"}
				""".formatted(Fixture.GLOW_ADMIN_ID))
				.andExpect(jsonPath("$.first_name").value("Anna"));
		create(mia + "/time-off", "staff_time_off", superAdmin, """
				{"starts_at": "2026-11-03T09:00", "ends_at": "2026-11-03T13:00"}
				""")
				.andExpect(jsonPath("$.note").value(nullValue()));
		create(GLOW + "/customers", "customers", superAdmin, """
				{"first_name": "Ella", "last_name": "Client", "notes": "  Likes tea.  "}
				""")
				.andExpect(jsonPath("$.notes").value("Likes tea."))
				.andExpect(jsonPath("$.business_name").value("Glow Beauty Studio"));
	}

	/**
	 * Each answer has a field the request didn't send, and an {@code updated_at}, which is null in the
	 * fixture and set by the database: the record was read back, not echoed.
	 */
	@Test
	void everyPutAnswersTheRecordAsStored() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);
		String mia = GLOW + "/staff/" + Fixture.GLOW_STAFF;

		replace(GLOW, superAdmin, """
				{"name": "Glow Beauty Studio", "description": "Now with brows.", "image": "https://example.com/glow.png",
				 "currency": "EUR", "timezone": "Europe/Berlin"}
				""")
				.andExpect(jsonPath("$.id").value(Fixture.GLOW))
				.andExpect(jsonPath("$.description").value("Now with brows."))
				.andExpect(jsonPath("$.status").value("APPROVED"))
				.andExpect(jsonPath("$.updated_at").value(notNullValue()));
		replace(GLOW + "/services/" + Fixture.GLOW_MANICURE, superAdmin, """
				{"name": "Gel Manicure", "duration_minutes": 40, "price": 30.00, "is_active": true}
				""")
				.andExpect(jsonPath("$.id").value(Fixture.GLOW_MANICURE))
				.andExpect(jsonPath("$.name").value("Gel Manicure"))
				.andExpect(jsonPath("$.duration_minutes").value(40))
				.andExpect(jsonPath("$.created_at").value(notNullValue()))
				.andExpect(jsonPath("$.updated_at").value(notNullValue()));
		replace(mia, superAdmin, """
				{"title": "Lead Stylist", "is_active": true, "hired_at": "2021-03-15"}
				""")
				.andExpect(jsonPath("$.id").value(Fixture.GLOW_STAFF))
				.andExpect(jsonPath("$.title").value("Lead Stylist"))
				.andExpect(jsonPath("$.first_name").value("Mia"))
				.andExpect(jsonPath("$.email").value(Fixture.GLOW_EMPLOYEE))
				.andExpect(jsonPath("$.updated_at").value(notNullValue()));
		replace(mia + "/time-off/" + Fixture.GLOW_STAFF_CHRISTMAS, superAdmin, """
				{"starts_at": "2026-12-23T00:00", "ends_at": "2026-12-27T00:00", "note": "Christmas, from the 23rd"}
				""")
				.andExpect(jsonPath("$.id").value(Fixture.GLOW_STAFF_CHRISTMAS))
				.andExpect(jsonPath("$.starts_at").value("2026-12-23T00:00"))
				.andExpect(jsonPath("$.note").value("Christmas, from the 23rd"))
				.andExpect(jsonPath("$.updated_at").value(notNullValue()));
		replace(GLOW + "/customers/" + Fixture.GLOW_CUSTOMER, superAdmin, """
				{"first_name": "Olivia", "last_name": "Client-Smith", "phone": "+49 30 7770001"}
				""")
				.andExpect(jsonPath("$.id").value(Fixture.GLOW_CUSTOMER))
				.andExpect(jsonPath("$.last_name").value("Client-Smith"))
				.andExpect(jsonPath("$.email").value(nullValue()))
				.andExpect(jsonPath("$.business_name").value("Glow Beauty Studio"))
				.andExpect(jsonPath("$.updated_at").value(notNullValue()));
		replace("/api/v1/users/" + Fixture.GLOW_EMPLOYEE_ID, superAdmin, """
				{"first_name": "Mia", "last_name": "Senior-Stylist", "role": "EMPLOYEE"}
				""")
				.andExpect(jsonPath("$.id").value(Fixture.GLOW_EMPLOYEE_ID))
				.andExpect(jsonPath("$.last_name").value("Senior-Stylist"))
				.andExpect(jsonPath("$.email").value(Fixture.GLOW_EMPLOYEE))
				.andExpect(jsonPath("$.addresses[0].street").value("3 Private Lane"))
				.andExpect(jsonPath("$.updated_at").value(notNullValue()));
		replace("/api/v1/addresses/" + Fixture.GLOW_EMPLOYEE_PERSONAL_ADDRESS, superAdmin, """
				{"street": "4 Private Lane", "city": "Berlin", "country": "Germany"}
				""")
				.andExpect(jsonPath("$.id").value(Fixture.GLOW_EMPLOYEE_PERSONAL_ADDRESS))
				.andExpect(jsonPath("$.street").value("4 Private Lane"))
				.andExpect(jsonPath("$.postal_code").value(nullValue()))
				.andExpect(jsonPath("$.user_id").value(Fixture.GLOW_EMPLOYEE_ID))
				.andExpect(jsonPath("$.updated_at").value(notNullValue()));
		replace("/api/v1/contacts/" + Fixture.GLOW_EMPLOYEE_PERSONAL_CONTACT, superAdmin, """
				{"type": "phone", "value": "+49 30 9990005"}
				""")
				.andExpect(jsonPath("$.id").value(Fixture.GLOW_EMPLOYEE_PERSONAL_CONTACT))
				.andExpect(jsonPath("$.value").value("+49 30 9990005"))
				.andExpect(jsonPath("$.created_at").value(notNullValue()))
				.andExpect(jsonPath("$.updated_at").value(notNullValue()));
	}

	@Test
	void everyDeleteAnswers204WithNoBody() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);
		String mia = GLOW + "/staff/" + Fixture.GLOW_STAFF;

		// Time off first: once Mia is removed, her time off can't be reached.
		for (String path : new String[] {
				mia + "/time-off/" + Fixture.GLOW_STAFF_CHRISTMAS,
				mia,
				GLOW + "/services/" + Fixture.GLOW_MANICURE,
				GLOW + "/customers/" + Fixture.GLOW_CUSTOMER,
				"/api/v1/addresses/" + Fixture.GLOW_EMPLOYEE_PERSONAL_ADDRESS,
				"/api/v1/contacts/" + Fixture.GLOW_EMPLOYEE_PERSONAL_CONTACT,
				"/api/v1/users/" + Fixture.GLOW_EMPLOYEE_ID,
				"/api/v1/businesses/" + Fixture.SERENITY_PENDING}) {
			mvc.perform(delete(path).session(superAdmin))
					.andExpect(status().isNoContent())
					.andExpect(content().string(""));
		}
	}

	/** POSTs the body, and checks its address and that its created_at is the one in the database. */
	private ResultActions create(String path, String table, MockHttpSession session, String body) throws Exception
	{
		ResultActions result = mvc.perform(post(path).session(session).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
		String json = result.andReturn().getResponse().getContentAsString();
		long id = ((Number) JsonPath.read(json, "$.id")).longValue();
		result.andExpect(header().string("Location", endsWith(path + "/" + id)));
		Timestamp stored = jdbcTemplate.queryForObject("SELECT created_at FROM " + table + " WHERE id = ?",
				Timestamp.class, id);
		assertThat(Instant.parse(JsonPath.read(json, "$.created_at"))).isEqualTo(stored.toInstant());
		return result;
	}

	private ResultActions replace(String path, MockHttpSession session, String body) throws Exception
	{
		return mvc.perform(put(path).session(session).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk());
	}
}
