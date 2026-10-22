package com.example.salon.service;

import com.example.salon.support.IntegrationTest;
import com.example.salon.support.RecordingMailer.Mail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A salon's admins hear the outcome of its review by email, as they can't sign in to see it. */
class SalonDecisionMailTest extends IntegrationTest
{
	@Autowired
	private JdbcTemplate jdbcTemplate;

	private MockHttpSession superAdmin;

	/** Serenity, pending, gets two admins and an employee, all with an email. */
	@BeforeEach
	void staffSerenity() throws Exception
	{
		String insert = """
				INSERT INTO users (first_name, last_name, role, email, password_hash, business_id)
				SELECT ?, 'Serenity', ?, ?, password_hash, ? FROM users WHERE id = ?""";
		jdbcTemplate.update(insert, "Sara", "ADMIN", "sara@serenity.test", Fixture.SERENITY_PENDING, Fixture.SUPER_ADMIN_ID);
		jdbcTemplate.update(insert, "Omar", "ADMIN", "omar@serenity.test", Fixture.SERENITY_PENDING, Fixture.SUPER_ADMIN_ID);
		jdbcTemplate.update(insert, "Ella", "EMPLOYEE", "ella@serenity.test", Fixture.SERENITY_PENDING, Fixture.SUPER_ADMIN_ID);
		superAdmin = loginAs(Fixture.SUPER_ADMIN);
	}

	@Test
	void approvingASalonEmailsItsAdminsALinkToSignIn() throws Exception
	{
		setStatus(Fixture.SERENITY_PENDING, "APPROVED");

		assertThat(mailer.sent()).extracting(Mail::to).containsExactly("sara@serenity.test", "omar@serenity.test");
		Mail mail = mailer.sent().getFirst();
		assertThat(mail.subject()).isEqualTo("Serenity Day Spa is approved on Salonat Pro");
		assertThat(mail.text()).startsWith("Hi Sara,").contains("http://localhost:3000/signin");
	}

	@Test
	void rejectingAPendingSalonEmailsItsAdminsAndApprovingItLaterDoesToo() throws Exception
	{
		setStatus(Fixture.SERENITY_PENDING, "REJECTED");

		assertThat(mailer.sent()).extracting(Mail::subject)
				.containsOnly("Serenity Day Spa wasn't approved on Salonat Pro")
				.hasSize(2);
		assertThat(mailer.sent().getFirst().text()).contains("wasn't approved").doesNotContain("http");

		mailer.clear();
		setStatus(Fixture.SERENITY_PENDING, "APPROVED");
		assertThat(mailer.sent()).extracting(Mail::subject).containsOnly("Serenity Day Spa is approved on Salonat Pro");
	}

	@Test
	void suspendingReinstatingOrNotChangingSendsNothing() throws Exception
	{
		setStatus(Fixture.GLOW, "SUSPENDED");
		setStatus(Fixture.GLOW, "APPROVED");
		setStatus(Fixture.GLOW, "APPROVED");
		setStatus(Fixture.SERENITY_PENDING, "PENDING");

		assertThat(mailer.sent()).isEmpty();
	}

	private void setStatus(long businessId, String status) throws Exception
	{
		mvc.perform(put("/api/v1/businesses/" + businessId + "/status").session(superAdmin)
						.contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"" + status + "\"}"))
				.andExpect(status().isOk());
	}
}
