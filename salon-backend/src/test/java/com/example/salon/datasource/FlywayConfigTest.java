package com.example.salon.datasource;

import com.example.salon.support.IntegrationTest;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Flyway, as the app configures it, run against a scratch schema of the test database: it migrates a
 * database it keeps the history of, or an empty one, and refuses anything it can't account for (BE-38).
 */
class FlywayConfigTest extends IntegrationTest
{
	private static final String SCRATCH = "flyway_scratch";

	@Autowired
	private Flyway flyway;

	@Autowired
	private HikariDataSource dataSource;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void dropScratchSchema()
	{
		jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + SCRATCH + " CASCADE");
	}

	@Test
	void aDatabaseWithTablesButNoMigrationHistoryIsRefusedNotBaselined()
	{
		// Say the wrong database, or a restore that lost flyway_schema_history.
		jdbcTemplate.execute("CREATE SCHEMA " + SCRATCH);
		jdbcTemplate.execute("CREATE TABLE " + SCRATCH + ".orders (id int)");

		try (HikariDataSource scratch = scratchDataSource()) {
			Flyway onScratch = Flyway.configure().configuration(flyway.getConfiguration()).dataSource(scratch).load();

			assertThatThrownBy(onScratch::migrate)
					.isInstanceOf(FlywayException.class)
					.hasMessageContaining("no schema history table");
		}
		assertThat(jdbcTemplate.queryForObject(
				"SELECT count(*) FROM information_schema.tables WHERE table_schema = ?", Integer.class, SCRATCH))
				.isEqualTo(1);
	}

	@Test
	void aMigrationFileFlywayCantParseStopsTheMigrationInsteadOfBeingSkipped(@TempDir Path migrations) throws IOException
	{
		Files.writeString(migrations.resolve("V14_one_underscore_short.sql"), "CREATE TABLE never_made (id int);");
		jdbcTemplate.execute("CREATE SCHEMA " + SCRATCH);

		try (HikariDataSource scratch = scratchDataSource()) {
			Flyway onScratch = Flyway.configure().configuration(flyway.getConfiguration()).dataSource(scratch)
					.locations("filesystem:" + migrations).load();

			assertThatThrownBy(onScratch::migrate)
					.isInstanceOf(FlywayException.class)
					.hasMessageContaining("V14_one_underscore_short.sql");
		}
	}

	/** The test database, with the scratch schema as the one Flyway works in. */
	private HikariDataSource scratchDataSource()
	{
		String url = dataSource.getJdbcUrl();
		HikariDataSource scratch = new HikariDataSource();
		scratch.setJdbcUrl(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCRATCH);
		scratch.setUsername(dataSource.getUsername());
		scratch.setPassword(dataSource.getPassword());
		scratch.setMaximumPoolSize(2); // Flyway holds one connection for its lock and one to migrate.
		return scratch;
	}
}
