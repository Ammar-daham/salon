package com.example.salon.datasource;

import com.example.salon.support.IntegrationTest;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Flyway, as the app configures it, run against a scratch schema of the test database: it migrates a
 * database it keeps the history of, or an empty one, and refuses anything it can't account for.
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

	/** Rows that staff removed before V14 left behind; a current staff member's stay. */
	@Test
	void v14ClearsTheShiftsServicesAndTimeOffOfStaffRemovedBefore() throws SQLException
	{
		jdbcTemplate.execute("CREATE SCHEMA " + SCRATCH);

		try (HikariDataSource scratch = scratchDataSource()) {
			Flyway.configure().configuration(flyway.getConfiguration()).dataSource(scratch).target("13").load()
					.migrate();
			try (Connection connection = scratch.getConnection()) {
				ScriptUtils.executeSqlScript(connection, new ClassPathResource("fixtures/seed.sql"));
			}
			JdbcTemplate onScratch = new JdbcTemplate(scratch);
			// Mia removed as before V14, and Leo with a shift of his own.
			onScratch.update("UPDATE staff SET deleted_at = now() WHERE id = ?", Fixture.GLOW_STAFF);
			onScratch.update("""
					INSERT INTO staff_schedules (staff_id, day_of_week, starts_at, ends_at) VALUES (?, 1, '09:00', '12:00')
					""", Fixture.URBAN_STAFF);

			Flyway.configure().configuration(flyway.getConfiguration()).dataSource(scratch).load().migrate();

			for (String table : new String[] {"staff_schedules", "staff_services", "staff_time_off"}) {
				assertThat(onScratch.queryForObject("SELECT count(*) FROM " + table + " WHERE staff_id = ?",
						Integer.class, Fixture.GLOW_STAFF)).as(table).isZero();
			}
			assertThat(onScratch.queryForObject("SELECT count(*) FROM staff_schedules WHERE staff_id = ?",
					Integer.class, Fixture.URBAN_STAFF)).isEqualTo(1);
			assertThat(onScratch.queryForObject("SELECT count(*) FROM staff WHERE id = ?", Integer.class,
					Fixture.GLOW_STAFF)).isEqualTo(1);
		}
	}

	/** Who performs a service removed before V15 is forgotten; who performs a current one isn't. */
	@Test
	void v15ClearsWhoPerformsAServiceRemovedBefore() throws SQLException
	{
		jdbcTemplate.execute("CREATE SCHEMA " + SCRATCH);

		try (HikariDataSource scratch = scratchDataSource()) {
			Flyway.configure().configuration(flyway.getConfiguration()).dataSource(scratch).target("14").load()
					.migrate();
			try (Connection connection = scratch.getConnection()) {
				ScriptUtils.executeSqlScript(connection, new ClassPathResource("fixtures/seed.sql"));
			}
			JdbcTemplate onScratch = new JdbcTemplate(scratch);
			// Mia does the manicure too, and the haircut is removed as before V15.
			onScratch.update("INSERT INTO staff_services (business_id, staff_id, service_id) VALUES (?, ?, ?)",
					Fixture.GLOW, Fixture.GLOW_STAFF, Fixture.GLOW_MANICURE);
			onScratch.update("UPDATE services SET deleted_at = now() WHERE id = ?", Fixture.GLOW_HAIRCUT);

			Flyway.configure().configuration(flyway.getConfiguration()).dataSource(scratch).load().migrate();

			assertThat(onScratch.queryForList("SELECT service_id FROM staff_services WHERE staff_id = ?",
					Long.class, Fixture.GLOW_STAFF)).containsExactly(Fixture.GLOW_MANICURE);
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
