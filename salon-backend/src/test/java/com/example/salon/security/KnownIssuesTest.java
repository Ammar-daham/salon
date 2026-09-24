package com.example.salon.security;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Open findings from salon-backend/AUDIT_FINDINGS.md, written as the behaviour we want.
 * They fail today, so each is disabled until the branch named in its reason lands; that branch
 * removes the @Disabled. Run them anyway with: ./gradlew test -PrunKnownIssues
 */
class KnownIssuesTest extends IntegrationTest
{
	@Test
	@Disabled("BE-41: fixed by fix/salon-backend-address-contact-ownership (VERSION_CONTROL_GUIDE.md §9, row 0.13)")
	void ownersCanReadTheirOwnAddressAndContact() throws Exception
	{
		MockHttpSession owner = loginAs(Fixture.GLOW_EMPLOYEE);

		mvc.perform(get("/api/v1/addresses/" + Fixture.GLOW_EMPLOYEE_PERSONAL_ADDRESS).session(owner))
				.andExpect(status().isOk());
		mvc.perform(get("/api/v1/contacts/" + Fixture.GLOW_EMPLOYEE_PERSONAL_CONTACT).session(owner))
				.andExpect(status().isOk());
	}

	@Test
	@Disabled("BE-41: fixed by fix/salon-backend-address-contact-ownership (VERSION_CONTROL_GUIDE.md §9, row 0.13)")
	void anySignedInUserCanReadASalonsPublicAddressAndContact() throws Exception
	{
		MockHttpSession otherSalonEmployee = loginAs(Fixture.URBAN_EMPLOYEE);

		mvc.perform(get("/api/v1/addresses/" + Fixture.GLOW_ADDRESS).session(otherSalonEmployee))
				.andExpect(status().isOk());
		mvc.perform(get("/api/v1/contacts/" + Fixture.GLOW_CONTACT).session(otherSalonEmployee))
				.andExpect(status().isOk());
	}

	@Test
	@Disabled("BE-01: fixed by fix/salon-backend-user-scoping (VERSION_CONTROL_GUIDE.md §9, row 0.6)")
	void adminOnlySeesUsersOfTheirOwnBusiness() throws Exception
	{
		mvc.perform(get("/api/v1/users").session(loginAs(Fixture.GLOW_ADMIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].email", hasItem(Fixture.GLOW_EMPLOYEE)))
				.andExpect(jsonPath("$[*].email", not(hasItem(Fixture.URBAN_ADMIN))))
				.andExpect(jsonPath("$[*].email", not(hasItem(Fixture.SUPER_ADMIN))));
	}

	@Test
	@Disabled("BE-05: fixed by fix/salon-backend-user-scoping (VERSION_CONTROL_GUIDE.md §9, row 0.6)")
	void adminCannotEditUsersOfAnotherSalon() throws Exception
	{
		mvc.perform(put("/api/v1/users/" + Fixture.URBAN_ADMIN_ID).session(loginAs(Fixture.GLOW_ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"first_name": "Pwned", "last_name": "Admin", "role": "EMPLOYEE"}
								"""))
				.andExpect(status().isForbidden());

		mvc.perform(get("/api/v1/users/" + Fixture.URBAN_ADMIN_ID).session(loginAs(Fixture.URBAN_ADMIN)))
				.andExpect(jsonPath("$.first_name").value("Ben"))
				.andExpect(jsonPath("$.role").value("ADMIN"));
	}
}
