package com.example.salon.controller;

import com.example.salon.model.NotificationType;
import com.example.salon.service.NotificationService;
import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Each user's own notifications: read, counted and marked read by them alone. */
class NotificationControllerTest extends IntegrationTest
{
	private static final String NOTIFICATIONS = "/api/v1/notifications";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private NotificationService notificationService;

	/** Sam has Serenity's (1, unread, newer) and Glow's (2, read); Anna has one of her own (3). */
	@BeforeEach
	void seedNotifications()
	{
		JdbcTemplate db = jdbcTemplate;
		String insert = """
				INSERT INTO notifications (id, user_id, type, business_id, created_at, read_at)
				VALUES (?, ?, 'BUSINESS_REGISTERED', ?, ?::timestamptz, ?::timestamptz)""";
		db.update(insert, 1, Fixture.SUPER_ADMIN_ID, Fixture.SERENITY_PENDING, "2026-10-21T09:00:00Z", null);
		db.update(insert, 2, Fixture.SUPER_ADMIN_ID, Fixture.GLOW, "2026-10-20T09:00:00Z", "2026-10-20T10:00:00Z");
		db.update(insert, 3, Fixture.GLOW_ADMIN_ID, Fixture.GLOW, "2026-10-21T08:00:00Z", null);
		db.queryForObject("SELECT setval(pg_get_serial_sequence('notifications', 'id'), 3)", Long.class);
	}

	@Test
	void aUserListsTheirOwnNotificationsNewestFirst() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);

		mvc.perform(get(NOTIFICATIONS).session(superAdmin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total_items").value(2))
				.andExpect(jsonPath("$.items[*].id", contains(1, 2)))
				.andExpect(jsonPath("$.items[0].type").value("BUSINESS_REGISTERED"))
				.andExpect(jsonPath("$.items[0].business.id").value(Fixture.SERENITY_PENDING))
				.andExpect(jsonPath("$.items[0].business.name").value("Serenity Day Spa"))
				.andExpect(jsonPath("$.items[0].created_at").value("2026-10-21T09:00:00Z"))
				.andExpect(jsonPath("$.items[0].read_at").value(nullValue()))
				.andExpect(jsonPath("$.items[1].read_at").value("2026-10-20T10:00:00Z"));
		mvc.perform(get(NOTIFICATIONS + "?unread=true").session(superAdmin))
				.andExpect(jsonPath("$.items[*].id", contains(1)));
		mvc.perform(get(NOTIFICATIONS + "/unread-count").session(superAdmin))
				.andExpect(content().json("{\"count\": 1}"));

		mvc.perform(get(NOTIFICATIONS).session(loginAs(Fixture.GLOW_ADMIN)))
				.andExpect(jsonPath("$.items[*].id", contains(3)));
		mvc.perform(get(NOTIFICATIONS).session(loginAs(Fixture.GLOW_EMPLOYEE)))
				.andExpect(jsonPath("$.total_items").value(0));
		mvc.perform(get(NOTIFICATIONS + "?unread=maybe").session(superAdmin)).andExpect(status().isBadRequest());
	}

	@Test
	void aUserMarksTheirOwnReadAndNoOneElses() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);

		// Anna's: a 404, as one that doesn't exist, and it stays unread.
		mvc.perform(put(NOTIFICATIONS + "/3/read").session(superAdmin)).andExpect(status().isNotFound());
		mvc.perform(put(NOTIFICATIONS + "/99/read").session(superAdmin)).andExpect(status().isNotFound());
		mvc.perform(get(NOTIFICATIONS + "/unread-count").session(loginAs(Fixture.GLOW_ADMIN)))
				.andExpect(jsonPath("$.count").value(1));

		mvc.perform(put(NOTIFICATIONS + "/1/read").session(superAdmin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.read_at").value(notNullValue()));
		// Already read: it keeps the time it first was.
		mvc.perform(put(NOTIFICATIONS + "/2/read").session(superAdmin))
				.andExpect(jsonPath("$.read_at").value("2026-10-20T10:00:00Z"));
		mvc.perform(get(NOTIFICATIONS + "/unread-count").session(superAdmin)).andExpect(jsonPath("$.count").value(0));
	}

	@Test
	void aUserMarksAllTheirsReadAtOnce() throws Exception
	{
		jdbcTemplate.update("UPDATE notifications SET read_at = NULL WHERE id = 2");
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);

		mvc.perform(put(NOTIFICATIONS + "/read").session(superAdmin))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		mvc.perform(get(NOTIFICATIONS + "/unread-count").session(superAdmin)).andExpect(jsonPath("$.count").value(0));
		mvc.perform(get(NOTIFICATIONS + "/unread-count").session(loginAs(Fixture.GLOW_ADMIN)))
				.andExpect(jsonPath("$.count").value(1));
	}

	@Test
	void everySuperAdminIsToldAndNoOneElse() throws Exception
	{
		jdbcTemplate.update("""
				INSERT INTO users (first_name, last_name, role, email, password_hash)
				SELECT 'Kim', 'Root', 'SUPER_ADMIN', 'root2@salon.test', password_hash FROM users WHERE id = ?
				""", Fixture.SUPER_ADMIN_ID);

		notificationService.notifySuperAdmins(NotificationType.BUSINESS_REGISTERED, Fixture.URBAN);

		mvc.perform(get(NOTIFICATIONS + "?unread=true").session(loginAs(Fixture.SUPER_ADMIN)))
				.andExpect(jsonPath("$.items[*].business.name", contains("Urban Cuts Barbershop", "Serenity Day Spa")));
		mvc.perform(get(NOTIFICATIONS).session(loginAs("root2@salon.test")))
				.andExpect(jsonPath("$.items[*].business.id", contains((int) Fixture.URBAN)));
		mvc.perform(get(NOTIFICATIONS + "/unread-count").session(loginAs(Fixture.GLOW_ADMIN)))
				.andExpect(jsonPath("$.count").value(1));
	}

	@Test
	void aRemovedSalonTakesItsNotificationsWithIt() throws Exception
	{
		mvc.perform(delete("/api/v1/businesses/" + Fixture.SERENITY_PENDING).session(loginAs(Fixture.SUPER_ADMIN)))
				.andExpect(status().isNoContent());

		mvc.perform(get(NOTIFICATIONS).session(loginAs(Fixture.SUPER_ADMIN)))
				.andExpect(jsonPath("$.items[*].id", contains(2)));
	}

	@Test
	void itNeedsASession() throws Exception
	{
		mvc.perform(get(NOTIFICATIONS)).andExpect(status().isUnauthorized());
		mvc.perform(get(NOTIFICATIONS + "/unread-count")).andExpect(status().isUnauthorized());
		mvc.perform(put(NOTIFICATIONS + "/read")).andExpect(status().isUnauthorized());
	}
}
