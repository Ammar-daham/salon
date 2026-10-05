package com.example.salon.datasource;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Rules the database enforces on its own, whatever the DAOs do. */
class SchemaConstraintsTest extends IntegrationTest
{
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void addressesAndContactsNeedExactlyOneOwner()
	{
		// DB-06
		assertThatThrownBy(() -> jdbcTemplate.update(
				"INSERT INTO contacts (type, value) VALUES ('phone', '+49 30 0000000')"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("contacts_one_owner_check");
		assertThatThrownBy(() -> jdbcTemplate.update(
				"INSERT INTO addresses (street, city, country, business_id, user_id) VALUES ('x', 'x', 'x', ?, ?)",
				Fixture.GLOW, Fixture.GLOW_EMPLOYEE_ID))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("addresses_one_owner_check");
	}

	@Test
	void deletingAnOwnerDeletesItsAddressesAndContacts()
	{
		// With the owner CHECK, ON DELETE SET NULL would fail; the FKs cascade instead.
		jdbcTemplate.update("DELETE FROM users WHERE id = ?", Fixture.GLOW_EMPLOYEE_ID);

		assertThat(count("contacts", Fixture.GLOW_EMPLOYEE_PERSONAL_CONTACT)).isZero();
		assertThat(count("addresses", Fixture.GLOW_EMPLOYEE_PERSONAL_ADDRESS)).isZero();
	}

	@Test
	void deletingASalonDeletesItsCustomers()
	{
		// DB-03: a salon's customer list goes with it. Urban has staff (BE-17), so unlink them first.
		jdbcTemplate.update("DELETE FROM staff WHERE business_id = ?", Fixture.URBAN);
		jdbcTemplate.update("DELETE FROM users WHERE business_id = ?", Fixture.URBAN);
		jdbcTemplate.update("DELETE FROM businesses WHERE id = ?", Fixture.URBAN);

		assertThat(count("customers", Fixture.URBAN_CUSTOMER)).isZero();
		assertThat(count("customers", Fixture.GLOW_CUSTOMER)).isOne();
	}

	@Test
	void everyTimestampColumnCarriesATimeZone()
	{
		// DB-09: no "timestamp without time zone" left anywhere in the schema.
		List<String> naive = jdbcTemplate.queryForList("""
				SELECT table_name || '.' || column_name FROM information_schema.columns
				WHERE table_schema = 'public' AND data_type = 'timestamp without time zone'
				AND table_name <> 'flyway_schema_history'
				""", String.class);
		assertThat(naive).isEmpty();
	}

	@Test
	void theDatabaseRejectsBadMoneyAndCoordinates()
	{
		// DB-07 / DB-08: the CHECKs hold even for writes that skip the API's validation.
		assertThatThrownBy(() -> jdbcTemplate.update("UPDATE services SET price = -1 WHERE id = ?", Fixture.GLOW_HAIRCUT))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("services_price_non_negative_check");
		assertThatThrownBy(() -> jdbcTemplate.update("UPDATE businesses SET currency = 'eur' WHERE id = ?", Fixture.GLOW))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("businesses_currency_format_check");
		assertThatThrownBy(() -> jdbcTemplate.update("UPDATE addresses SET latitude = 52.5 WHERE id = ?", Fixture.GLOW_ADDRESS))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("addresses_coordinates_pair_check");
	}

	@Test
	void theDatabaseRejectsBackwardsOrOverlappingOpeningHours()
	{
		// DB-14: seed.sql opens Glow on Tuesday (2) 09:00-18:00.
		String insert = "INSERT INTO business_hours (business_id, day_of_week, opens_at, closes_at) VALUES (?, ?, ?::time, ?::time)";

		assertThatThrownBy(() -> jdbcTemplate.update(insert, Fixture.GLOW, 1, "18:00", "09:00"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("business_hours_order_check");
		assertThatThrownBy(() -> jdbcTemplate.update(insert, Fixture.GLOW, 8, "09:00", "18:00"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("business_hours_day_of_week_check");
		assertThatThrownBy(() -> jdbcTemplate.update(insert, Fixture.GLOW, 2, "17:00", "19:00"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("business_hours_no_overlap");

		// Touching the existing interval, or the same times at another salon, is allowed.
		jdbcTemplate.update(insert, Fixture.GLOW, 2, "18:00", "20:00");
		jdbcTemplate.update(insert, Fixture.URBAN, 2, "09:00", "18:00");
	}

	@Test
	void deletingASalonDeletesItsOpeningHours()
	{
		// Serenity has no staff, so it can be deleted (BE-17).
		jdbcTemplate.update("INSERT INTO business_hours VALUES (?, 1, '09:00', '17:00')", Fixture.SERENITY_PENDING);
		jdbcTemplate.update("DELETE FROM businesses WHERE id = ?", Fixture.SERENITY_PENDING);

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM business_hours WHERE business_id = ?",
				Integer.class, Fixture.SERENITY_PENDING)).isZero();
	}

	@Test
	void aStaffMemberCanOnlyBeLinkedToTheirOwnSalonsServices()
	{
		// DB-14: business_id is part of both foreign keys. Mia is Glow's; the fade is Urban's.
		String link = "INSERT INTO staff_services (business_id, staff_id, service_id) VALUES (?, ?, ?)";

		assertThatThrownBy(() -> jdbcTemplate.update(link, Fixture.GLOW, Fixture.GLOW_STAFF, Fixture.URBAN_FADE))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("fk_staff_services_service");
		assertThatThrownBy(() -> jdbcTemplate.update(link, Fixture.URBAN, Fixture.GLOW_STAFF, Fixture.URBAN_FADE))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("fk_staff_services_staff");
		jdbcTemplate.update(link, Fixture.GLOW, Fixture.GLOW_STAFF, Fixture.GLOW_MANICURE);
	}

	@Test
	void theDatabaseRejectsBackwardsOrOverlappingShifts()
	{
		// seed.sql: Mia works Tuesday (2) 09:00-17:00.
		String shift = "INSERT INTO staff_schedules (staff_id, day_of_week, starts_at, ends_at) VALUES (?, ?, ?::time, ?::time)";

		assertThatThrownBy(() -> jdbcTemplate.update(shift, Fixture.GLOW_STAFF, 1, "17:00", "09:00"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("staff_schedules_order_check");
		assertThatThrownBy(() -> jdbcTemplate.update(shift, Fixture.GLOW_STAFF, 2, "16:00", "18:00"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("staff_schedules_no_overlap");

		// Touching is fine, and another staff member can work the same hours.
		jdbcTemplate.update(shift, Fixture.GLOW_STAFF, 2, "17:00", "19:00");
		jdbcTemplate.update(shift, Fixture.URBAN_STAFF, 2, "09:00", "17:00");
	}

	@Test
	void removingAStaffMemberOrAServiceRemovesWhatHungOffIt()
	{
		jdbcTemplate.update("DELETE FROM services WHERE id = ?", Fixture.GLOW_HAIRCUT);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM staff_services", Integer.class)).isZero();

		jdbcTemplate.update("DELETE FROM staff WHERE id = ?", Fixture.GLOW_STAFF);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM staff_schedules", Integer.class)).isZero();
	}

	private int count(String table, long id)
	{
		return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table + " WHERE id = ?", Integer.class, id);
	}
}
