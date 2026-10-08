package com.example.salon.mail;

import com.example.salon.logging.RequestLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For when no SMTP server is configured, as in development: the email goes to the log instead, so a
 * reset link can be followed from there. Under the prod profile that would hand reset links to anyone
 * who can read the logs, so there it only logs that nothing was sent.
 */
public class LogMailer implements Mailer
{
	private static final Logger log = LoggerFactory.getLogger(LogMailer.class);

	private final boolean showEmails;

	public LogMailer(boolean showEmails)
	{
		this.showEmails = showEmails;
	}

	@Override
	public void send(String to, String subject, String text)
	{
		if (showEmails) {
			log.info("No SMTP server (spring.mail.host), so not sent. To {}: {}\n{}", to, subject, text);
		} else {
			log.error("\"{}\" to {} not sent: no SMTP server configured (spring.mail.host)", subject, RequestLog.maskEmail(to));
		}
	}
}
