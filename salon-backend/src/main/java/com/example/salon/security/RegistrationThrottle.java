package com.example.salon.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

/**
 * Limits how many salons are registered from one client address, since registering needs no session: by
 * default 5 in any hour ({@code app.registration.max-per-address} and {@code window}). See AddressThrottle.
 */
@Component
public class RegistrationThrottle extends AddressThrottle
{
	@Autowired
	public RegistrationThrottle(
			@Value("${app.registration.max-per-address:5}") int maxPerAddress,
			@Value("${app.registration.window:1h}") Duration window,
			Clock clock)
	{
		super(maxPerAddress, window, clock, "Too many salons registered from this address.");
	}
}
