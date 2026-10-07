package com.example.salon.security;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** BE-22: repeated failed sign-ins lock the email, and the address they come from. */
class LoginRateLimitingTest extends IntegrationTest
{
	private static final String HOME = "10.0.0.1";
	private static final String ELSEWHERE = "10.0.0.2";

	@Test
	void fiveWrongPasswordsLockTheAccountEvenAgainstTheRightOne() throws Exception
	{
		failTimes(5, Fixture.GLOW_ADMIN, HOME);

		signIn(Fixture.GLOW_ADMIN, Fixture.PASSWORD, HOME)
				.andExpect(status().isTooManyRequests())
				// 900 seconds from the fifth failure, so a moment less by now.
				.andExpect(result -> assertThat(Long.parseLong(result.getResponse().getHeader("Retry-After")))
						.isBetween(1L, 900L))
				.andExpect(jsonPath("$.errorCode").value("TOO_MANY_REQUESTS"))
				.andExpect(jsonPath("$.message").value("Too many failed sign-in attempts. Try again in 15 minutes."));

		// The account is locked, not just the address.
		signIn(Fixture.GLOW_ADMIN, Fixture.PASSWORD, ELSEWHERE).andExpect(status().isTooManyRequests());
	}

	@Test
	void aBurstSentAtOnceGetsNoMoreGuesses() throws Exception
	{
		// Each attempt counts as soon as it's let in, not once its password has been checked.
		ExecutorService pool = Executors.newFixedThreadPool(20);
		try {
			List<Future<Integer>> statuses = new ArrayList<>();
			for (int i = 0; i < 20; i++) {
				statuses.add(pool.submit(() ->
						signIn(Fixture.GLOW_ADMIN, "wrong-password", HOME).andReturn().getResponse().getStatus()));
			}
			List<Integer> answered = new ArrayList<>();
			for (Future<Integer> status : statuses) {
				answered.add(status.get());
			}
			assertThat(answered).filteredOn(s -> s == 401).hasSize(5);
			assertThat(answered).filteredOn(s -> s == 429).hasSize(15);
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	void anEmailWithNoAccountLocksTheSameWay() throws Exception
	{
		// Otherwise whether the sixth try is a 429 or a 401 would say whether the account exists.
		failTimes(5, "nobody@salon.test", HOME);

		signIn("nobody@salon.test", Fixture.PASSWORD, HOME).andExpect(status().isTooManyRequests());
	}

	@Test
	void aLockedAccountLeavesOthersAlone() throws Exception
	{
		failTimes(5, Fixture.GLOW_ADMIN, HOME);

		signIn(Fixture.GLOW_EMPLOYEE, Fixture.PASSWORD, HOME).andExpect(status().isOk());
	}

	@Test
	void signingInStartsTheCountAgain() throws Exception
	{
		failTimes(4, Fixture.GLOW_ADMIN, HOME);
		signIn(Fixture.GLOW_ADMIN, Fixture.PASSWORD, HOME).andExpect(status().isOk());
		failTimes(4, Fixture.GLOW_ADMIN, HOME);

		signIn(Fixture.GLOW_ADMIN, Fixture.PASSWORD, HOME).andExpect(status().isOk());
	}

	@Test
	void twentyFailuresFromOneAddressLockItOutForEveryAccount() throws Exception
	{
		for (int i = 0; i < 20; i++) {
			signIn("guess" + i + "@salon.test", Fixture.PASSWORD, HOME).andExpect(status().isUnauthorized());
		}

		signIn(Fixture.GLOW_ADMIN, Fixture.PASSWORD, HOME).andExpect(status().isTooManyRequests());
		signIn(Fixture.GLOW_ADMIN, Fixture.PASSWORD, ELSEWHERE).andExpect(status().isOk());
	}

	private void failTimes(int times, String email, String address) throws Exception
	{
		for (int i = 0; i < times; i++) {
			signIn(email, "wrong-password", address).andExpect(status().isUnauthorized());
		}
	}

	private ResultActions signIn(String email, String password, String address) throws Exception
	{
		return mvc.perform(post("/api/v1/auth/login")
				.with(request -> {
					request.setRemoteAddr(address);
					return request;
				})
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody(email, password)));
	}
}
