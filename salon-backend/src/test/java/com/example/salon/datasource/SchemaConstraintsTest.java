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

	private int count(String table, long id)
	{
		return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table + " WHERE id = ?", Integer.class, id);
	}
}
