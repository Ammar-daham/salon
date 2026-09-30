package com.example.salon.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;

@Configuration
public class JacksonConfig
{
	/**
	 * BE-23's request DTOs only expose the fields each endpoint actually reads. A client that still
	 * sends the wider shape it got back from GET (e.g. the admin panel echoing a nested "contacts"
	 * list on a business update, BE-06/BE-07) must keep being ignored, not rejected. A property in
	 * application.yml can't guarantee this - that file is gitignored and only exists where a
	 * developer copied it from application.yml_template, so this is set in code instead.
	 */
	@Bean
	public JsonMapperBuilderCustomizer ignoreUnknownJsonProperties()
	{
		return builder -> builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	}
}
