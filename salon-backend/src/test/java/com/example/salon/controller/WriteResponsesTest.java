package com.example.salon.controller;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every endpoint that changes a record answers the same way as the others of its kind: a DELETE
 * answers 204 with no body.
 */
class WriteResponsesTest extends IntegrationTest
{
	private static final String GLOW = "/api/v1/businesses/" + Fixture.GLOW;

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
}
