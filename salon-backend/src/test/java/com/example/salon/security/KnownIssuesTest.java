package com.example.salon.security;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

}
