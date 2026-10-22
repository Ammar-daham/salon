package com.example.salon.security;

import com.example.salon.exception.TooManyAttemptsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limits how many salons are registered from one client address, since registering needs no session: by
 * default 5 in any hour ({@code app.registration.max-per-address} and {@code window}). Every attempt counts,
 * whether or not it went through, so a script can't try name after name or email after email to find which
 * are taken. Like LoginThrottle, it counts in memory: counts start again on a restart, each server instance
 * counts its own, and behind a reverse proxy the address is the proxy's unless forwarded headers are
 * trusted.
 */
@Component
public class RegistrationThrottle
{
	/** Past this many addresses, the ones with nothing left in the window are forgotten. */
	private static final int SWEEP_ABOVE = 10_000;

	private final int maxPerAddress;
	private final Duration window;
	private final Clock clock;
	private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

	@Autowired
	public RegistrationThrottle(
			@Value("${app.registration.max-per-address:5}") int maxPerAddress,
			@Value("${app.registration.window:1h}") Duration window,
			Clock clock)
	{
		this.maxPerAddress = maxPerAddress;
		this.window = window;
		this.clock = clock;
	}

	/** Counts one registration from the address, or refuses it with a 429 after too many lately. */
	public void admit(String address)
	{
		Instant now = clock.instant();
		Instant since = now.minus(window);
		if (attempts.size() > SWEEP_ABOVE)
			sweep(since);
		// compute runs atomically for the address; throwing leaves its count as it was.
		attempts.compute(address, (key, times) -> {
			Deque<Instant> recent = times != null ? times : new ArrayDeque<>();
			forgetBefore(recent, since);
			if (recent.size() >= maxPerAddress)
				throw new TooManyAttemptsException("Too many salons registered from this address.",
						Duration.between(now, recent.peekFirst().plus(window)));
			recent.addLast(now);
			return recent;
		});
	}

	/** Forgets every count. The tests share one instance, and each starts with none. */
	public void reset()
	{
		attempts.clear();
	}

	private void sweep(Instant since)
	{
		for (String address : attempts.keySet()) {
			attempts.computeIfPresent(address, (key, times) -> {
				forgetBefore(times, since);
				return times.isEmpty() ? null : times;
			});
		}
	}

	private static void forgetBefore(Deque<Instant> times, Instant since)
	{
		while (!times.isEmpty() && !times.peekFirst().isAfter(since))
			times.pollFirst();
	}
}
