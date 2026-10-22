package com.example.salon.security;

import com.example.salon.exception.TooManyAttemptsException;
import com.example.salon.support.TestClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/** At most three registrations from one address in an hour, here; each address counts its own. */
class RegistrationThrottleTest
{
	private static final Instant NINE = Instant.parse("2026-10-22T09:00:00Z");

	private final TestClock clock = new TestClock();
	private final RegistrationThrottle throttle = new RegistrationThrottle(3, Duration.ofHours(1), clock);

	@Test
	void theFourthFromOneAddressWithinTheWindowIsRefused()
	{
		for (int minute = 0; minute < 3; minute++) {
			clock.stopAt(NINE.plusSeconds(minute * 60L));
			throttle.admit("203.0.113.7");
		}

		clock.stopAt(NINE.plusSeconds(30 * 60));
		TooManyAttemptsException refused =
				catchThrowableOfType(TooManyAttemptsException.class, () -> throttle.admit("203.0.113.7"));

		// Until the first of the three is an hour old.
		assertThat(refused.getRetryAfterSeconds()).isEqualTo(30 * 60);
		assertThat(refused.getMessage())
				.isEqualTo("Too many salons registered from this address. Try again in 30 minutes.");
		throttle.admit("198.51.100.4");
	}

	@Test
	void aRefusedAttemptDoesntCountAndTheWindowMovesOn()
	{
		clock.stopAt(NINE);
		for (int i = 0; i < 3; i++)
			throttle.admit("203.0.113.7");
		assertThatThrownBy(() -> throttle.admit("203.0.113.7")).isInstanceOf(TooManyAttemptsException.class);

		clock.stopAt(NINE.plus(Duration.ofHours(1)).plusSeconds(1));
		for (int i = 0; i < 3; i++)
			throttle.admit("203.0.113.7");
		assertThatThrownBy(() -> throttle.admit("203.0.113.7")).isInstanceOf(TooManyAttemptsException.class);
	}

	@Test
	void resetForgetsEveryCount()
	{
		clock.stopAt(NINE);
		for (int i = 0; i < 3; i++)
			throttle.admit("203.0.113.7");

		throttle.reset();

		throttle.admit("203.0.113.7");
	}
}
