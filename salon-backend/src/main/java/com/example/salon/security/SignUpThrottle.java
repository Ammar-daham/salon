package com.example.salon.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

/**
 * Limits how many client accounts are created from one client address, since signing up needs no session:
 * by default 5 in any hour ({@code app.sign-up.max-per-address} and {@code window}), as for registering a
 * salon. It counts apart from RegistrationThrottle. See AddressThrottle.
 */
@Component
public class SignUpThrottle extends AddressThrottle
{
	@Autowired
	public SignUpThrottle(
			@Value("${app.sign-up.max-per-address:5}") int maxPerAddress,
			@Value("${app.sign-up.window:1h}") Duration window,
			Clock clock)
	{
		super(maxPerAddress, window, clock, "Too many accounts created from this address.");
	}
}
