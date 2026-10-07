package com.example.salon.security;

import com.example.salon.exception.TooManyAttemptsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The counting and timing behind BE-22, on a clock the test moves. LoginRateLimitingTest covers the endpoint. */
class LoginThrottleTest
{
	private static final String ANNA = "anna@glow.test";
	private static final String HOME = "10.0.0.1";
	private static final String ELSEWHERE = "10.0.0.2";

	private final MovableClock clock = new MovableClock();
	private final LoginThrottle throttle = new LoginThrottle(5, 20, Duration.ofMinutes(15), clock);

	@Test
	void fiveFailuresOnOneEmailLockItUntilTheFirstIsAWindowOld()
	{
		failTimes(4, ANNA, HOME);
		assertThat(throttle.lockedFor(ANNA, HOME)).isEmpty();

		failTimes(1, ANNA, HOME);
		assertThat(throttle.lockedFor(ANNA, HOME)).contains(Duration.ofMinutes(15));
		// From anywhere: it's the account that's locked.
		assertThat(throttle.lockedFor(ANNA, ELSEWHERE)).contains(Duration.ofMinutes(15));
		assertThatThrownBy(() -> throttle.admit(ANNA, ELSEWHERE))
				.isInstanceOf(TooManyAttemptsException.class)
				.hasMessage("Too many failed sign-in attempts. Try again in 15 minutes.");

		clock.advance(Duration.ofMinutes(10));
		assertThat(throttle.lockedFor(ANNA, HOME)).contains(Duration.ofMinutes(5));

		clock.advance(Duration.ofMinutes(5));
		assertThat(throttle.lockedFor(ANNA, HOME)).isEmpty();
	}

	@Test
	void theWindowSlides()
	{
		failTimes(1, ANNA, HOME);
		clock.advance(Duration.ofMinutes(10));
		failTimes(4, ANNA, HOME);

		// Open again once the first failure is 15 minutes old, with room for one more.
		assertThat(throttle.lockedFor(ANNA, HOME)).contains(Duration.ofMinutes(5));
		clock.advance(Duration.ofMinutes(5));
		failTimes(1, ANNA, HOME);
		assertThat(throttle.lockedFor(ANNA, HOME)).contains(Duration.ofMinutes(10));
	}

	@Test
	void attemptsStillBeingCheckedCountAlready()
	{
		// A burst sent at once: none has failed yet, but only five get through.
		List<LoginThrottle.Attempt> inFlight = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			inFlight.add(throttle.admit(ANNA, HOME));
		}

		assertThatThrownBy(() -> throttle.admit(ANNA, HOME)).isInstanceOf(TooManyAttemptsException.class);
	}

	@Test
	void theEmailIsCountedWhateverItsCase()
	{
		failTimes(3, ANNA, HOME);
		failTimes(2, ANNA.toUpperCase(), HOME);

		assertThat(throttle.lockedFor("Anna@Glow.test", HOME)).isPresent();
	}

	@Test
	void aSuccessClearsTheEmailsFailures()
	{
		failTimes(4, ANNA, HOME);
		throttle.admit(ANNA, HOME).succeeded();

		failTimes(4, ANNA, HOME);
		assertThat(throttle.lockedFor(ANNA, HOME)).isEmpty();
	}

	@Test
	void twentyFailuresFromOneAddressLockItForEveryEmail()
	{
		for (int i = 0; i < 20; i++) {
			failTimes(1, "guess" + i + "@glow.test", HOME);
		}

		assertThat(throttle.lockedFor(ANNA, HOME)).contains(Duration.ofMinutes(15));
		assertThat(throttle.lockedFor(ANNA, ELSEWHERE)).isEmpty();
	}

	@Test
	void aRefusedAddressDoesNotCountAgainstTheEmail()
	{
		for (int i = 0; i < 20; i++) {
			failTimes(1, "guess" + i + "@glow.test", HOME);
		}
		for (int i = 0; i < 10; i++) {
			assertThatThrownBy(() -> throttle.admit(ANNA, HOME)).isInstanceOf(TooManyAttemptsException.class);
		}

		throttle.admit(ANNA, ELSEWHERE).succeeded();
	}

	@Test
	void successesDoNotCountAgainstTheAddress()
	{
		// A whole salon signing in from behind one router.
		for (int i = 0; i < 25; i++) {
			throttle.admit("staff" + i + "@glow.test", HOME).succeeded();
		}

		assertThat(throttle.lockedFor(ANNA, HOME)).isEmpty();
	}

	@Test
	void aSuccessLeavesTheAddressesOtherFailures()
	{
		for (int i = 0; i < 19; i++) {
			failTimes(1, "guess" + i + "@glow.test", HOME);
		}
		throttle.admit(ANNA, HOME).succeeded();

		failTimes(1, "guess19@glow.test", HOME);
		assertThat(throttle.lockedFor(ANNA, HOME)).isPresent();
	}

	@Test
	void aMissingEmailStillCountsAgainstTheAddress()
	{
		for (int i = 0; i < 20; i++) {
			failTimes(1, i % 2 == 0 ? null : " ", HOME);
		}

		assertThat(throttle.lockedFor(null, HOME)).isPresent();
		assertThat(throttle.lockedFor(ANNA, ELSEWHERE)).isEmpty();
	}

	@Test
	void expiredCountsAreSweptOnceTooManyAreTracked()
	{
		// Each a different email from a different address, as a spray of guesses would be.
		for (int i = 0; i <= LoginThrottle.SWEEP_ABOVE; i++) {
			failTimes(1, "guess" + i + "@glow.test", "10.1." + i / 256 + "." + i % 256);
		}
		clock.advance(Duration.ofMinutes(15));

		failTimes(1, ANNA, HOME);
		assertThat(throttle.tracked()).isEqualTo(2);
	}

	private void failTimes(int times, String email, String address)
	{
		for (int i = 0; i < times; i++) {
			throttle.admit(email, address).failed();
		}
	}

	private static final class MovableClock extends Clock
	{
		private Instant now = Instant.parse("2026-10-07T10:00:00Z");

		void advance(Duration duration)
		{
			now = now.plus(duration);
		}

		@Override
		public Instant instant()
		{
			return now;
		}

		@Override
		public ZoneId getZone()
		{
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone)
		{
			throw new UnsupportedOperationException();
		}
	}
}
