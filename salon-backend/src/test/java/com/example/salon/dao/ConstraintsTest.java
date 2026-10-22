package com.example.salon.dao;

import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;

/** Constraints reads the constraint Postgres names in its error, whatever the message around it says. */
class ConstraintsTest
{
	@Test
	void itNamesTheConstraintPostgresReports()
	{
		DataIntegrityViolationException ex = refused("appointments_no_double_booking",
				"conflicting key value violates exclusion constraint \"appointments_no_double_booking\"");

		assertThat(Constraints.violated(ex, "appointments_no_double_booking")).isTrue();
		assertThat(Constraints.violated(ex, "staff_time_off_no_overlap")).isFalse();
	}

	@Test
	void aConstraintNamedOnlyInTheMessageDoesntCount()
	{
		// The detail quotes the value that clashed, here a contact that happens to read like a constraint.
		DataIntegrityViolationException ex = refused("contacts_user_value_unique_idx",
				"duplicate key value violates unique constraint \"contacts_user_value_unique_idx\"\n"
						+ "  Detail: Key (user_id, value)=(4, appointments_no_double_booking) already exists.");

		assertThat(ex.getMessage()).contains("appointments_no_double_booking");
		assertThat(Constraints.violated(ex, "appointments_no_double_booking")).isFalse();
	}

	@Test
	void anErrorThatIsntPostgresRefusingAWriteNamesNone()
	{
		DataIntegrityViolationException ex = new DataIntegrityViolationException("appointments_no_double_booking",
				new PSQLException("appointments_no_double_booking", PSQLState.UNKNOWN_STATE));

		assertThat(Constraints.violated(ex, "appointments_no_double_booking")).isFalse();
		assertThat(Constraints.violated(new DataIntegrityViolationException("appointments_no_double_booking"),
				"appointments_no_double_booking")).isFalse();
	}

	/** As Spring wraps the error Postgres sends: S severity, C SQLSTATE, M message, n constraint. */
	private static DataIntegrityViolationException refused(String constraint, String message)
	{
		ServerErrorMessage error = new ServerErrorMessage("SERROR\0C23000\0M" + message + "\0n" + constraint + "\0");
		return new DataIntegrityViolationException(message, new PSQLException(error));
	}
}
