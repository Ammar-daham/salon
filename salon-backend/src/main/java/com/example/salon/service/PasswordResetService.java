package com.example.salon.service;

import com.example.salon.dao.PasswordResetTokenDao;
import com.example.salon.dao.UserDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.logging.RequestLog;
import com.example.salon.mail.Mailer;
import com.example.salon.model.User;
import com.example.salon.security.LoginThrottle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Resets a forgotten password through a link sent by email (FE-13). The link carries a random token,
 * of which only a SHA-256 is stored; it works once, until it expires, and asking again retires the
 * earlier links. A user is sent at most a few an hour, so the form can't be used to flood a mailbox.
 */
@Service
public class PasswordResetService
{
	private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

	private static final SecureRandom RANDOM = new SecureRandom();
	/** Links older than this are deleted, used or not, whenever a new one is sent. */
	private static final Duration KEPT_FOR = Duration.ofDays(1);

	private final UserDao userDao;
	private final UserService userService;
	private final PasswordResetTokenDao tokenDao;
	private final Mailer mailer;
	private final LoginThrottle loginThrottle;
	private final TaskExecutor executor;
	private final String link;
	private final Duration linkValidFor;
	private final int maxPerHour;

	@Autowired
	public PasswordResetService(UserDao userDao, UserService userService, PasswordResetTokenDao tokenDao, Mailer mailer,
			LoginThrottle loginThrottle, TaskExecutor executor,
			@Value("${app.password-reset.link:http://localhost:3000/reset-password}") String link,
			@Value("${app.password-reset.valid-for:1h}") Duration linkValidFor,
			@Value("${app.password-reset.max-per-hour:3}") int maxPerHour)
	{
		this.userDao = userDao;
		this.userService = userService;
		this.tokenDao = tokenDao;
		this.mailer = mailer;
		this.loginThrottle = loginThrottle;
		this.executor = executor;
		this.link = link;
		this.linkValidFor = linkValidFor;
		this.maxPerHour = maxPerHour;
	}

	/**
	 * Sends a reset link to the account with this email, if there is one that can sign in. The work
	 * happens in the background and this returns at once, so the answer takes as long, and says as
	 * little, whether or not the email has an account.
	 */
	public void requestReset(String email)
	{
		executor.execute(() -> {
			try {
				sendLink(email);
			} catch (RuntimeException ex) {
				log.error("Couldn't send a password reset link to {}", RequestLog.maskEmail(email), ex);
			}
		});
	}

	/** Sets a new password with the token from a reset link, and signs out every session of the user. */
	@Transactional
	public void resetPassword(String token, String password)
	{
		Instant now = Instant.now();
		// A link stays usable only while its user can sign in: it mustn't give a removed login back.
		User user = tokenDao.redeem(hash(token), now)
				.flatMap(userDao::findById)
				.filter(u -> u.getPasswordHash() != null)
				.orElseThrow(() -> new BaseException("This reset link is invalid or has expired. Ask for a new one.",
						ErrorCode.BAD_REQUEST));

		tokenDao.retireAll(user.getId(), now);
		userService.setPassword(user.getId(), password);
		// Whoever reset it has the mailbox; a lock run up by someone guessing shouldn't keep them out.
		loginThrottle.clear(user.getEmail());
		log.info("User {} reset their password", user.getId());
	}

	private void sendLink(String email)
	{
		Optional<User> found = userDao.findByEmail(email).filter(u -> u.getPasswordHash() != null);
		if (found.isEmpty()) {
			log.info("Password reset asked for {}, which has no account that can sign in", RequestLog.maskEmail(email));
			return;
		}
		User user = found.get();

		Instant now = Instant.now();
		if (tokenDao.countIssuedSince(user.getId(), now.minus(Duration.ofHours(1))) >= maxPerHour) {
			log.warn("Password reset link for user {} not sent: {} already sent in the last hour", user.getId(), maxPerHour);
			return;
		}

		tokenDao.deleteIssuedBefore(now.minus(KEPT_FOR));
		String token = newToken();
		tokenDao.issue(user.getId(), hash(token), now, now.plus(linkValidFor));
		mailer.send(user.getEmail(), "Reset your Salon Admin password", emailText(user, link + "?token=" + token));
		log.info("Password reset link sent to user {}", user.getId());
	}

	private String emailText(User user, String url)
	{
		return """
				Hi %s,

				Someone asked to reset the password for your Salon Admin account. To choose a new one, open this \
				link within %d minutes:

				%s

				If it wasn't you, ignore this email and your password stays as it is.
				""".formatted(user.getFirstName(), linkValidFor.toMinutes(), url);
	}

	/** 256 random bits, URL-safe. */
	private static String newToken()
	{
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** What's stored instead of the token: a slow hash isn't needed for 256 random bits. */
	private static String hash(String token)
	{
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("Every Java runtime has SHA-256", ex);
		}
	}
}
