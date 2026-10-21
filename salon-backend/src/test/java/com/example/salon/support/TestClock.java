package com.example.salon.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** The app's clock in tests: the system's until a test stops it at an instant. IntegrationTest restarts it. */
public class TestClock extends Clock
{
	private volatile Instant stoppedAt;

	public void stopAt(Instant instant)
	{
		stoppedAt = instant;
	}

	public void restart()
	{
		stoppedAt = null;
	}

	@Override
	public Instant instant()
	{
		Instant stopped = stoppedAt;
		return stopped != null ? stopped : Instant.now();
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

	@TestConfiguration
	public static class Config
	{
		@Bean
		@Primary
		public TestClock testClock()
		{
			return new TestClock();
		}
	}
}
