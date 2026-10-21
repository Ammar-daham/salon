package com.example.salon.dto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

/**
 * The rule for every password the API sets, in {@link CreateUserRequest}, {@link ChangePasswordRequest} and
 * {@link ResetPasswordRequest}: 8 to 72 characters, and at most 72 bytes in UTF-8. 72 bytes is BCrypt's
 * input limit, and BCryptPasswordEncoder throws past it rather than cut the password short. A letter outside
 * English takes 2 bytes or more, so 40 × "ä" is 40 characters but 80 bytes, and is refused with its own
 * message. Null is valid; pair with {@code @NotBlank} where the password is required.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Password.Validator.class)
public @interface Password
{
	String message() default "must be between 8 and 72 characters";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	class Validator implements ConstraintValidator<Password, String>
	{
		private static final int MIN_LENGTH = 8;
		private static final int MAX_BYTES = 72;

		@Override
		public boolean isValid(String value, ConstraintValidatorContext context)
		{
			if (value == null)
				return true;
			if (value.length() < MIN_LENGTH || value.length() > MAX_BYTES)
				return false;
			if (value.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES)
				return true;
			// Few enough characters, too many bytes: say why, or "between 8 and 72 characters" would puzzle.
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate(
					"must be at most 72 bytes in UTF-8, where a letter outside English takes 2 or more")
					.addConstraintViolation();
			return false;
		}
	}
}
