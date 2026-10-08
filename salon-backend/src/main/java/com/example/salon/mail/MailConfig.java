package com.example.salon.mail;

import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskDecorator;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;

@Configuration
public class MailConfig
{
	/** Spring Boot only makes a JavaMailSender once spring.mail.host is set. */
	@Bean
	public Mailer mailer(ObjectProvider<JavaMailSender> smtp, Environment environment,
			@Value("${app.mail.from:Salon Admin <no-reply@localhost>}") String from)
	{
		JavaMailSender sender = smtp.getIfAvailable();
		return sender != null ? new SmtpMailer(sender, from) : new LogMailer(!environment.matchesProfiles("prod"));
	}

	/**
	 * Carries the request id onto the background thread that sends a reset link, so its log lines can
	 * be found with the request's (BE-42). Spring Boot applies it to its applicationTaskExecutor.
	 */
	@Bean
	public TaskDecorator requestIdTaskDecorator()
	{
		return task -> {
			Map<String, String> context = MDC.getCopyOfContextMap();
			return () -> {
				if (context != null) {
					MDC.setContextMap(context);
				}
				try {
					task.run();
				} finally {
					MDC.clear();
				}
			};
		};
	}
}
