package com.example.salon.dao;

import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataAccessException;

/**
 * Which database constraint a write broke, from the field Postgres names it in, rather than found in the
 * exception's message. The message is for people: its wording can change between driver versions, and it
 * quotes the values that clashed, which may themselves contain a constraint's name.
 */
public final class Constraints
{
	private Constraints()
	{
	}

	/** Whether Postgres refused the write that threw ex because it would break the named constraint. */
	public static boolean violated(DataAccessException ex, String constraint)
	{
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (cause instanceof PSQLException psql) {
				ServerErrorMessage error = psql.getServerErrorMessage();
				return error != null && constraint.equals(error.getConstraint());
			}
		}
		return false;
	}
}
