package com.example.salon.mail;

import com.example.salon.logging.RequestLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** Through the SMTP server in spring.mail.*. A failure is logged, not thrown: the caller has already answered. */
public class SmtpMailer implements Mailer
{
	private static final Logger log = LoggerFactory.getLogger(SmtpMailer.class);

	private final JavaMailSender sender;
	private final String from;

	public SmtpMailer(JavaMailSender sender, String from)
	{
		this.sender = sender;
		this.from = from;
	}

	@Override
	public void send(String to, String subject, String text)
	{
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(to);
		message.setSubject(subject);
		message.setText(text);
		try {
			sender.send(message);
			log.info("Sent \"{}\" to {}", subject, RequestLog.maskEmail(to));
		} catch (MailException ex) {
			log.error("Couldn't send \"{}\" to {}", subject, RequestLog.maskEmail(to), ex);
		}
	}
}
