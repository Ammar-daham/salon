package com.example.salon.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Repository
public class PasswordResetTokenDataAccessService implements PasswordResetTokenDao
{
	private final JdbcTemplate jdbcTemplate;

	@Autowired
	public PasswordResetTokenDataAccessService(JdbcTemplate jdbcTemplate)
	{
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public int countIssuedSince(long userId, Instant since)
	{
		Integer count = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM password_reset_tokens WHERE user_id = ? AND created_at > ?",
				Integer.class, userId, at(since));
		return count != null ? count : 0;
	}

	@Override
	public void issue(long userId, String tokenHash, Instant now, Instant expiresAt)
	{
		// One statement, so a link is never stored without the earlier ones being retired.
		String sql = """
				WITH retired AS (
				    UPDATE password_reset_tokens SET used_at = ? WHERE user_id = ? AND used_at IS NULL
				)
				INSERT INTO password_reset_tokens (user_id, token_hash, created_at, expires_at) VALUES (?, ?, ?, ?)
				""";
		jdbcTemplate.update(sql, at(now), userId, userId, tokenHash, at(now), at(expiresAt));
	}

	@Override
	public Optional<Long> redeem(String tokenHash, Instant now)
	{
		// Checking and using it in one statement means two requests can't both use the same link.
		String sql = """
				UPDATE password_reset_tokens SET used_at = ?
				WHERE token_hash = ? AND used_at IS NULL AND expires_at > ?
				RETURNING user_id
				""";
		return jdbcTemplate.queryForList(sql, Long.class, at(now), tokenHash, at(now)).stream().findFirst();
	}

	@Override
	public void retireAll(long userId, Instant now)
	{
		jdbcTemplate.update("UPDATE password_reset_tokens SET used_at = ? WHERE user_id = ? AND used_at IS NULL",
				at(now), userId);
	}

	@Override
	public void deleteIssuedBefore(Instant before)
	{
		jdbcTemplate.update("DELETE FROM password_reset_tokens WHERE created_at < ?", at(before));
	}

	private static OffsetDateTime at(Instant instant)
	{
		return instant.atOffset(ZoneOffset.UTC);
	}
}
