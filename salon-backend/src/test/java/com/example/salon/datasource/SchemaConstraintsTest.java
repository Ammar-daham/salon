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
	void theDatabaseRejectsBackwardsOrOverlappingShiftsAndTimeOff()
	{
		// seed.sql: Mia works Tuesday (2) 09:00-17:00 and is off 24-27 December.
		String shift = "INSERT INTO staff_schedules (staff_id, day_of_week, starts_at, ends_at) VALUES (?, ?, ?::time, ?::time)";
		String timeOff = "INSERT INTO staff_time_off (staff_id, starts_at, ends_at) VALUES (?, ?::timestamptz, ?::timestamptz)";

		assertThatThrownBy(() -> jdbcTemplate.update(shift, Fixture.GLOW_STAFF, 1, "17:00", "09:00"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("staff_schedules_order_check");
		assertThatThrownBy(() -> jdbcTemplate.update(shift, Fixture.GLOW_STAFF, 2, "16:00", "18:00"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("staff_schedules_no_overlap");
		assertThatThrownBy(() -> jdbcTemplate.update(timeOff, Fixture.GLOW_STAFF, "2026-12-26 12:00+01", "2026-12-28 00:00+01"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("staff_time_off_no_overlap");
		assertThatThrownBy(() -> jdbcTemplate.update(timeOff, Fixture.GLOW_STAFF, "2027-01-02 00:00+01", "2027-01-01 00:00+01"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("staff_time_off_order_check");

		// Another staff member can be off at the same time.
		jdbcTemplate.update(timeOff, Fixture.URBAN_STAFF, "2026-12-24 00:00+01", "2026-12-27 00:00+01");
	}

	@Test
	void removingAStaffMemberOrAServiceRemovesWhatHungOffIt()
	{
		jdbcTemplate.update("DELETE FROM services WHERE id = ?", Fixture.GLOW_HAIRCUT);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM staff_services", Integer.class)).isZero();

		jdbcTemplate.update("DELETE FROM staff WHERE id = ?", Fixture.GLOW_STAFF);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM staff_schedules", Integer.class)).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM staff_time_off", Integer.class)).isZero();
	}

	private static final String APPOINTMENT = """
			INSERT INTO appointments (business_id, customer_id, staff_id, service_id, starts_at, ends_at, status, price)
			VALUES (?, ?, ?, ?, ?::timestamptz, ?::timestamptz, ?, 45.00)
			""";

	@Test
	void theDatabaseRefusesToDoubleBookAStaffMember()
	{
		// DB-14: Mia has a haircut 10:00-10:45.
		jdbcTemplate.update(APPOINTMENT, Fixture.GLOW, Fixture.GLOW_CUSTOMER, Fixture.GLOW_STAFF, Fixture.GLOW_HAIRCUT,
				"2027-03-02 10:00+01", "2027-03-02 10:45+01", "BOOKED");

		assertThatThrownBy(() -> jdbcTemplate.update(APPOINTMENT, Fixture.GLOW, Fixture.GLOW_CUSTOMER, Fixture.GLOW_STAFF,
				Fixture.GLOW_HAIRCUT, "2027-03-02 10:30+01", "2027-03-02 11:15+01", "CONFIRMED"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("appointments_no_double_booking");
		assertThatThrownBy(() -> jdbcTemplate.update(APPOINTMENT, Fixture.GLOW, Fixture.GLOW_CUSTOMER, Fixture.GLOW_STAFF,
				Fixture.GLOW_HAIRCUT, "2027-03-02 12:00+01", "2027-03-02 11:00+01", "BOOKED"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("appointments_order_check");
		assertThatThrownBy(() -> jdbcTemplate.update(APPOINTMENT, Fixture.GLOW, Fixture.GLOW_CUSTOMER, Fixture.GLOW_STAFF,
				Fixture.GLOW_HAIRCUT, "2027-03-02 14:00+01", "2027-03-02 14:45+01", "LATE"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("appointments_status_check");

		// Back to back is fine, and a cancelled or missed appointment doesn't hold its time.
		jdbcTemplate.update(APPOINTMENT, Fixture.GLOW, Fixture.GLOW_CUSTOMER, Fixture.GLOW_STAFF, Fixture.GLOW_HAIRCUT,
				"2027-03-02 10:45+01", "2027-03-02 11:30+01", "BOOKED");
		jdbcTemplate.update(APPOINTMENT, Fixture.GLOW, Fixture.GLOW_CUSTOMER, Fixture.GLOW_STAFF, Fixture.GLOW_HAIRCUT,
				"2027-03-02 10:00+01", "2027-03-02 10:45+01", "CANCELLED");
		jdbcTemplate.update(APPOINTMENT, Fixture.GLOW, Fixture.GLOW_CUSTOMER, Fixture.GLOW_STAFF, Fixture.GLOW_HAIRCUT,
				"2027-03-02 10:00+01", "2027-03-02 10:45+01", "NO_SHOW");
	}

	@Test
	void anAppointmentCanOnlyPointAtItsOwnSalonsCustomerStaffAndService()
	{
		// DB-14: business_id is part of every foreign key. The customer, staff member and fade are Urban's.
		String at = "2027-03-02 10:00+01";
		String until = "2027-03-02 10:45+01";

		assertThatThrownBy(() -> jdbcTemplate.update(APPOINTMENT, Fixture.GLOW, Fixture.URBAN_CUSTOMER, Fixture.GLOW_STAFF,
				Fixture.GLOW_HAIRCUT, at, until, "BOOKED"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("fk_appointments_customer");
		assertThatThrownBy(() -> jdbcTemplate.update(APPOINTMENT, Fixture.GLOW, Fixture.GLOW_CUSTOMER, Fixture.URBAN_STAFF,
				Fixture.GLOW_HAIRCUT, at, until, "BOOKED"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("fk_appointments_staff");
		assertThatThrownBy(() -> jdbcTemplate.update(APPOINTMENT, Fixture.GLOW, Fixture.GLOW_CUSTOMER, Fixture.GLOW_STAFF,
				Fixture.URBAN_FADE, at, until, "BOOKED"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("fk_appointments_service");
	}

	@Test
	void whatAnAppointmentPointsAtCanOnlyBeSoftDeleted()
	{
		// DB-13: the API stamps deleted_at; deleting the row outright is refused while an appointment needs it.
		jdbcTemplate.update(APPOINTMENT, Fixture.GLOW, Fixture.GLOW_CUSTOMER, Fixture.GLOW_STAFF, Fixture.GLOW_HAIRCUT,
				"2025-03-04 10:00+01", "2025-03-04 10:45+01", "COMPLETED");

		assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM customers WHERE id = ?", Fixture.GLOW_CUSTOMER))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("fk_appointments_customer");
		assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM staff WHERE id = ?", Fixture.GLOW_STAFF))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("fk_appointments_staff");
		assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM services WHERE id = ?", Fixture.GLOW_HAIRCUT))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("fk_appointments_service");
	}

	@Test
	void deletingASalonDeletesItsAppointments()
	{
		// Serenity has no staff of its own (BE-17), so Glow's admin works there for this test.
		long service = jdbcTemplate.queryForObject("""
				INSERT INTO services (business_id, name, duration_minutes, price) VALUES (?, 'Massage', 60, 80.00)
				RETURNING id""", Long.class, Fixture.SERENITY_PENDING);
		long customer = jdbcTemplate.queryForObject("""
				INSERT INTO customers (business_id, first_name, last_name) VALUES (?, 'Ada', 'Client') RETURNING id""",
				Long.class, Fixture.SERENITY_PENDING);
		long staff = jdbcTemplate.queryForObject("""
				INSERT INTO staff (user_id, business_id, title, is_active) VALUES (?, ?, 'Therapist', true) RETURNING id""",
				Long.class, Fixture.GLOW_ADMIN_ID, Fixture.SERENITY_PENDING);
		jdbcTemplate.update(APPOINTMENT, Fixture.SERENITY_PENDING, customer, staff, service,
				"2027-03-02 10:00+01", "2027-03-02 11:00+01", "BOOKED");

		// Staff, services and customers go with the salon too, and the appointments' plain foreign keys to them
		// are checked once the whole delete is done, by which time the appointments are gone as well.
		jdbcTemplate.update("DELETE FROM businesses WHERE id = ?", Fixture.SERENITY_PENDING);

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM appointments", Integer.class)).isZero();
	}

	private int count(String table, long id)
	{
		return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table + " WHERE id = ?", Integer.class, id);
	}
}
