package com.example.salon.dao;

import java.time.Instant;
import java.util.Optional;

/** Password reset links, by the SHA-256 of their token (FE-13). */
public interface PasswordResetTokenDao
{
	int countIssuedSince(long userId, Instant since);

	/** Stores a new link for the user and retires every earlier one they haven't used. */
	void issue(long userId, String tokenHash, Instant now, Instant expiresAt);

	/** Uses a link that hasn't been used or retired and hasn't expired, and says whose it was. */
	Optional<Long> redeem(String tokenHash, Instant now);

	void retireAll(long userId, Instant now);

	/** Forgets links sent before then, used or not. */
	void deleteIssuedBefore(Instant before);
}
