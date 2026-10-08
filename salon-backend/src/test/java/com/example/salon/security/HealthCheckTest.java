package com.example.salon.security;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.registry.HealthContributorRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A load balancer can ask whether the app is up, without signing in, and learn nothing else (BE-39). */
class HealthCheckTest extends IntegrationTest
{
	@Autowired
	private HealthContributorRegistry healthContributors;

	@Test
	void anyoneCanAskWhetherTheAppIsUpButNotWhy() throws Exception
	{
		mvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components").doesNotExist());
	}

	@Test
	void theAnswerIncludesWhetherTheDatabaseResponds()
	{
		assertThat(healthContributors.getContributor("db")).isNotNull();
	}

	@Test
	void noOtherActuatorEndpointIsReachable() throws Exception
	{
		for (String path : new String[] {"/actuator", "/actuator/env", "/actuator/beans", "/actuator/heapdump"}) {
			mvc.perform(get(path)).andExpect(status().isUnauthorized());
			mvc.perform(get(path).session(loginAs(Fixture.SUPER_ADMIN))).andExpect(status().isForbidden());
		}
	}
}
