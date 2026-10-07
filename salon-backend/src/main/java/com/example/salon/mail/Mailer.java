package com.example.salon.mail;

/** Sends plain-text email. MailConfig picks SMTP when spring.mail.host is set, and the log otherwise. */
public interface Mailer
{
	void send(String to, String subject, String text);
}
