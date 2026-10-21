package com.example.salon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Where "now" comes from: the system clock, which a test can replace to stand at a given instant. */
@Configuration
public class ClockConfig
{
	@Bean
	public Clock clock()
	{
		return Clock.systemUTC();
	}
}
