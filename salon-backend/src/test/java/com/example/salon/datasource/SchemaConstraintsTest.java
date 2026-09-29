package com.example.salon.datasource;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

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

	private int count(String table, long id)
	{
		return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table + " WHERE id = ?", Integer.class, id);
	}
}
