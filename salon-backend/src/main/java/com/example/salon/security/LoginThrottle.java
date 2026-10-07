package com.example.salon.security;

import com.example.salon.exception.TooManyAttemptsException;
import com.example.salon.logging.RequestLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Failed sign-ins, counted per email and per client address (BE-22). An email gets a few tries in
 * any window, and an address more, across every email it tries; past that, sign-in is refused until
 * the oldest of them is a window old. The refusal comes before the password is checked, so while
 * locked a right guess gets the same answer as a wrong one.
 *
 * An attempt counts as a failure from the moment it's let through until it turns out to have
 * succeeded, so a burst sent all at once can't slip past the limit while the first ones are still
 * being checked. Every email is counted, whether or not it has an account, so the lockout can't be
 * used to find out which ones do. The counts live in memory: per instance, and gone on restart.
 */
@Component
public class LoginThrottle
{
	private static final Logger log = LoggerFactory.getLogger(LoginThrottle.class);

	/** Past this many emails or addresses tracked, expired ones are swept out on the next attempt. */
	static final int SWEEP_ABOVE = 10_000;

	private final int maxFailuresPerAccount;
	private final int maxFailuresPerAddress;
	private final Duration window;
	private final Clock clock;

	/** When each attempt still counting against an email or address was let through, oldest first. */
	private final ConcurrentMap<String, List<Instant>> accounts = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, List<Instant>> addresses = new ConcurrentHashMap<>();

	@Autowired
	public LoginThrottle(
			@Value("${app.login-throttle.max-failures-per-account:5}") int maxFailuresPerAccount,
			@Value("${app.login-throttle.max-failures-per-address:20}") int maxFailuresPerAddress,
			@Value("${app.login-throttle.window:15m}") Duration window)
	{
		this(maxFailuresPerAccount, maxFailuresPerAddress, window, Clock.systemUTC());
	}

	LoginThrottle(int maxFailuresPerAccount, int maxFailuresPerAddress, Duration window, Clock clock)
	{
		this.maxFailuresPerAccount = maxFailuresPerAccount;
		this.maxFailuresPerAddress = maxFailuresPerAddress;
		this.window = window;
		this.clock = clock;
	}

	/**
	 * Lets one sign-in attempt through, counted as a failure until it {@linkplain Attempt#succeeded()
	 * succeeds}.
	 *
	 * @throws TooManyAttemptsException when the email or the address has had too many
	 */
	public Attempt admit(String email, String address)
	{
		Instant now = clock.instant();
		String account = accountKey(email);
		boolean admitted = charge(accounts, account, maxFailuresPerAccount, now);
		if (admitted && !charge(addresses, address, maxFailuresPerAddress, now)) {
			refund(accounts, account, now);
			admitted = false;
		}
		if (!admitted) {
			throw new TooManyAttemptsException(lockedFor(email, address).orElse(Duration.ZERO));
		}
		return new Attempt(account, email, address, now);
	}

	/** How long until this email may try again from this address, or empty if it may now. */
	public Optional<Duration> lockedFor(String email, String address)
	{
		Instant now = clock.instant();
		Duration wait = longer(
				waitFor(accounts, accountKey(email), maxFailuresPerAccount, now),
				waitFor(addresses, address, maxFailuresPerAddress, now));
		return wait.isPositive() ? Optional.of(wait) : Optional.empty();
	}

	/** Forgets every attempt. For tests, which share one instance. */
	public void reset()
	{
		accounts.clear();
		addresses.clear();
	}

	/** How many emails and addresses are tracked, so tests can see the sweep. */
	int tracked()
	{
		return accounts.size() + addresses.size();
	}

	/** One sign-in that was let through. */
	public final class Attempt
	{
		private final String account;
		private final String email;
		private final String address;
		private final Instant at;

		private Attempt(String account, String email, String address, Instant at)
		{
			this.account = account;
			this.email = email;
			this.address = address;
			this.at = at;
		}

		/** It already counts; this logs the lock when it's the failure that reached the limit. */
		public void failed()
		{
			Instant now = clock.instant();
			if (account != null && recent(accounts.get(account), now).size() == maxFailuresPerAccount) {
				log.warn("Sign-in locked for {} after {} failures in {}", RequestLog.maskEmail(email), maxFailuresPerAccount, shown(window));
			}
			if (address != null && recent(addresses.get(address), now).size() == maxFailuresPerAddress) {
				log.warn("Sign-in locked for ip={} after {} failures in {}", address, maxFailuresPerAddress, shown(window));
			}
		}

		/** A right password clears the email's failures, and takes this one attempt off the address's. */
		public void succeeded()
		{
			if (account != null) {
				accounts.remove(account);
			}
			refund(addresses, address, at);
		}
	}

	/** Counts an attempt against the key, unless it has had too many already; says whether it did. */
	private boolean charge(ConcurrentMap<String, List<Instant>> counts, String key, int max, Instant now)
	{
		if (key == null) {
			return true;
		}
		AtomicBoolean charged = new AtomicBoolean();
		counts.compute(key, (k, times) -> {
			List<Instant> recent = recent(times, now);
			if (recent.size() >= max) {
				return recent;
			}
			charged.set(true);
			List<Instant> more = new ArrayList<>(recent);
			more.add(now);
			return List.copyOf(more);
		});
		if (counts.size() > SWEEP_ABOVE) {
			counts.values().removeIf(times -> recent(times, now).isEmpty());
		}
		return charged.get();
	}

	/** Takes back the attempt let through at {@code at}. */
	private static void refund(ConcurrentMap<String, List<Instant>> counts, String key, Instant at)
	{
		if (key == null) {
			return;
		}
		counts.computeIfPresent(key, (k, times) -> {
			List<Instant> fewer = new ArrayList<>(times);
			fewer.remove(at);
			return fewer.isEmpty() ? null : List.copyOf(fewer);
		});
	}

	/** Until enough of the key's attempts are a window old to let one more through. */
	private Duration waitFor(ConcurrentMap<String, List<Instant>> counts, String key, int max, Instant now)
	{
		List<Instant> recent = key != null ? recent(counts.get(key), now) : List.of();
		return recent.size() < max
				? Duration.ZERO
				: Duration.between(now, recent.get(recent.size() - max).plus(window));
	}

	/** The attempts that still count: those less than a window old. */
	private List<Instant> recent(List<Instant> times, Instant now)
	{
		if (times == null) {
			return List.of();
		}
		Instant cutoff = now.minus(window);
		return times.stream().filter(at -> at.isAfter(cutoff)).toList();
	}

	private static Duration longer(Duration a, Duration b)
	{
		return a.compareTo(b) >= 0 ? a : b;
	}

	/** "15m" or "1h30m" rather than PT15M. */
	private static String shown(Duration duration)
	{
		return duration.toString().substring(2).toLowerCase(Locale.ROOT);
	}

	/** Sign-in ignores the email's case (DB-10), so ANNA@x and anna@x share a count. */
	private static String accountKey(String email)
	{
		return email == null || email.isBlank() ? null : email.toLowerCase(Locale.ROOT);
	}
}
