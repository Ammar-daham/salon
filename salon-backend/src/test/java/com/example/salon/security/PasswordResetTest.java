package com.example.salon.security;

import com.example.salon.support.IntegrationTest;
import com.example.salon.support.RecordingMailer.Mail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FE-13: a forgotten password is reset through a link sent by email. */
class PasswordResetTest extends IntegrationTest
{
	private static final String NEW_PASSWORD = "NewPassword456!";
	private static final String INVALID_LINK = "This reset link is invalid or has expired. Ask for a new one.";
	private static final Pattern LINK = Pattern.compile("http://localhost:3000/reset-password\\?token=([A-Za-z0-9_-]{43})");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void theEmailedLinkSetsANewPassword() throws Exception
	{
		forgot(Fixture.GLOW_EMPLOYEE).andExpect(status().isAccepted());

		Mail mail = onlyMail();
		assertThat(mail.to()).isEqualTo(Fixture.GLOW_EMPLOYEE);
		assertThat(mail.subject()).isEqualTo("Reset your Salon Admin password");
		assertThat(mail.text()).startsWith("Hi Mia,").contains("within 60 minutes");

		reset(tokenIn(mail), NEW_PASSWORD).andExpect(status().isNoContent());
		signIn(Fixture.GLOW_EMPLOYEE, Fixture.PASSWORD).andExpect(status().isUnauthorized());
		signIn(Fixture.GLOW_EMPLOYEE, NEW_PASSWORD).andExpect(status().isOk());
	}

	@Test
	void theAnswerIsTheSameWhetherOrNotTheEmailHasAnAccount() throws Exception
	{
		String known = forgot(Fixture.GLOW_EMPLOYEE).andExpect(status().isAccepted())
				.andReturn().getResponse().getContentAsString();
		String unknown = forgot("nobody@salon.test").andExpect(status().isAccepted())
				.andReturn().getResponse().getContentAsString();

		assertThat(unknown).isEqualTo(known);
		assertThat(mailer.sent()).extracting(Mail::to).containsExactly(Fixture.GLOW_EMPLOYEE);
	}

	@Test
	void theEmailIsMatchedWhateverItsCase() throws Exception
	{
		forgot(Fixture.GLOW_EMPLOYEE.toUpperCase()).andExpect(status().isAccepted());

		assertThat(onlyMail().to()).isEqualTo(Fixture.GLOW_EMPLOYEE);
	}

	@Test
	void aLinkWorksOnce() throws Exception
	{
		forgot(Fixture.GLOW_EMPLOYEE);
		String token = tokenIn(onlyMail());

		reset(token, NEW_PASSWORD).andExpect(status().isNoContent());
		reset(token, "AnotherPassword789!")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(INVALID_LINK));
		signIn(Fixture.GLOW_EMPLOYEE, NEW_PASSWORD).andExpect(status().isOk());
	}

	@Test
	void anExpiredLinkDoesNotWork() throws Exception
	{
		forgot(Fixture.GLOW_EMPLOYEE);
		jdbcTemplate.update("UPDATE password_reset_tokens SET created_at = now() - interval '2 hours', "
				+ "expires_at = now() - interval '1 hour'");

		reset(tokenIn(onlyMail()), NEW_PASSWORD)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(INVALID_LINK));
	}

	@Test
	void aMadeUpTokenDoesNotWork() throws Exception
	{
		reset("not-a-token-anyone-was-sent", NEW_PASSWORD)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(INVALID_LINK));
	}

	@Test
	void aNewerLinkRetiresTheOlderOne() throws Exception
	{
		forgot(Fixture.GLOW_EMPLOYEE);
		forgot(Fixture.GLOW_EMPLOYEE);
		List<Mail> sent = mailer.sent();
		assertThat(sent).hasSize(2);

		reset(tokenIn(sent.get(0)), NEW_PASSWORD).andExpect(status().isBadRequest());
		reset(tokenIn(sent.get(1)), NEW_PASSWORD).andExpect(status().isNoContent());
	}

	@Test
	void aUserIsSentAtMostThreeLinksAnHour() throws Exception
	{
		for (int i = 0; i < 5; i++) {
			forgot(Fixture.GLOW_EMPLOYEE).andExpect(status().isAccepted());
		}

		assertThat(mailer.sent()).hasSize(3);
	}

	@Test
	void onlyAHashOfTheTokenIsStored() throws Exception
	{
		forgot(Fixture.GLOW_EMPLOYEE);
		String token = tokenIn(onlyMail());

		String stored = jdbcTemplate.queryForObject("SELECT token_hash FROM password_reset_tokens", String.class);
		assertThat(stored).isEqualTo(sha256(token)).doesNotContain(token);
	}

	@Test
	void resettingSignsOutEverySession() throws Exception
	{
		MockHttpSession session = loginAs(Fixture.GLOW_EMPLOYEE);
		forgot(Fixture.GLOW_EMPLOYEE);

		reset(tokenIn(onlyMail()), NEW_PASSWORD).andExpect(status().isNoContent());

		mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isUnauthorized());
	}

	@Test
	void resettingLiftsASignInLock() throws Exception
	{
		for (int i = 0; i < 5; i++) {
			signIn(Fixture.GLOW_EMPLOYEE, "wrong-password").andExpect(status().isUnauthorized());
		}
		signIn(Fixture.GLOW_EMPLOYEE, Fixture.PASSWORD).andExpect(status().isTooManyRequests());
		forgot(Fixture.GLOW_EMPLOYEE);

		reset(tokenIn(onlyMail()), NEW_PASSWORD).andExpect(status().isNoContent());

		signIn(Fixture.GLOW_EMPLOYEE, NEW_PASSWORD).andExpect(status().isOk());
	}

	@Test
	void aShortPasswordIsRefusedAndTheLinkStillWorks() throws Exception
	{
		forgot(Fixture.GLOW_EMPLOYEE);
		String token = tokenIn(onlyMail());

		reset(token, "short")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("password must be between 8 and 72 characters"));
		reset(token, NEW_PASSWORD).andExpect(status().isNoContent());
	}

	@Test
	void aLinkCannotGiveARemovedLoginBack() throws Exception
	{
		forgot(Fixture.GLOW_EMPLOYEE);
		jdbcTemplate.update("UPDATE users SET password_hash = NULL WHERE id = ?", Fixture.GLOW_EMPLOYEE_ID);

		reset(tokenIn(onlyMail()), NEW_PASSWORD).andExpect(status().isBadRequest());
		assertThat(jdbcTemplate.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class,
				Fixture.GLOW_EMPLOYEE_ID)).isNull();
	}

	@Test
	void anAccountThatCannotSignInIsSentNothing() throws Exception
	{
		jdbcTemplate.update("UPDATE users SET password_hash = NULL WHERE id = ?", Fixture.GLOW_EMPLOYEE_ID);

		forgot(Fixture.GLOW_EMPLOYEE).andExpect(status().isAccepted()).andExpect(content().string(""));

		assertThat(mailer.sent()).isEmpty();
	}

	private ResultActions forgot(String email) throws Exception
	{
		return mvc.perform(post("/api/v1/auth/forgot-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email": "%s"}
						""".formatted(email)));
	}

	private ResultActions reset(String token, String password) throws Exception
	{
		return mvc.perform(post("/api/v1/auth/reset-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"token": "%s", "password": "%s"}
						""".formatted(token, password)));
	}

	private ResultActions signIn(String email, String password) throws Exception
	{
		return mvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody(email, password)));
	}

	private Mail onlyMail()
	{
		assertThat(mailer.sent()).hasSize(1);
		return mailer.sent().getFirst();
	}

	private static String tokenIn(Mail mail)
	{
		Matcher link = LINK.matcher(mail.text());
		assertThat(link.find()).as("a reset link in:%n%s", mail.text()).isTrue();
		return link.group(1);
	}

	private static String sha256(String text) throws Exception
	{
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
		return HexFormat.of().formatHex(digest);
	}
}
