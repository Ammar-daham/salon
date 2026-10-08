package com.example.salon.support;

import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.system.CapturedOutput;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Counts the SQL statements JdbcTemplate runs while an action runs, from the line it logs at DEBUG for
 * each one. Needs OutputCaptureExtension on the test class.
 */
public final class SqlStatements
{
	private static final String JDBC_TEMPLATE = "org.springframework.jdbc.core.JdbcTemplate";
	private static final Pattern STATEMENT =
			Pattern.compile("Executing (prepared SQL statement|SQL query \\[|SQL statement \\[|SQL update \\[)");

	public interface Action
	{
		void run() throws Exception;
	}

	private SqlStatements()
	{
	}

	public static int countDuring(CapturedOutput output, Action action) throws Exception
	{
		LoggingSystem logging = LoggingSystem.get(SqlStatements.class.getClassLoader());
		int before = count(output);
		logging.setLogLevel(JDBC_TEMPLATE, LogLevel.DEBUG);
		try {
			action.run();
		} finally {
			logging.setLogLevel(JDBC_TEMPLATE, null);
		}
		return count(output) - before;
	}

	private static int count(CapturedOutput output)
	{
		Matcher matcher = STATEMENT.matcher(output.getAll());
		int count = 0;
		while (matcher.find()) {
			count++;
		}
		return count;
	}
}
