package com.example.salon.dto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.ZoneId;
import java.util.Set;

/**
 * An IANA time zone ID such as "Europe/Berlin", as java.time knows it. Fixed offsets like "+01:00" are
 * rejected: they don't follow daylight saving time, so a salon's 09:00 would drift by an hour twice a year.
 * Null is valid; pair with @NotNull where the value is required.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TimeZoneId.Validator.class)
public @interface TimeZoneId
{
	String message() default "must be an IANA time zone ID, e.g. Europe/Berlin";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	class Validator implements ConstraintValidator<TimeZoneId, String>
	{
		private static final Set<String> ZONE_IDS = ZoneId.getAvailableZoneIds();

		@Override
		public boolean isValid(String value, ConstraintValidatorContext context)
		{
			return value == null || ZONE_IDS.contains(value);
		}
	}
}
