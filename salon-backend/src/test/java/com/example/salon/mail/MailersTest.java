package com.example.salon.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(OutputCaptureExtension.class)
class MailersTest
{
	private static final String LINK = "http://localhost:3000/reset-password?token=secret-token";

	@Test
	void smtpSendsPlainTextFromTheConfiguredAddress()
	{
		JavaMailSender sender = mock(JavaMailSender.class);

		new SmtpMailer(sender, "Salon Admin <no-reply@salon.test>").send("mia@glow.test", "Reset", LINK);

		ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(sender).send(sent.capture());
		assertThat(sent.getValue().getFrom()).isEqualTo("Salon Admin <no-reply@salon.test>");
		assertThat(sent.getValue().getTo()).containsExactly("mia@glow.test");
		assertThat(sent.getValue().getSubject()).isEqualTo("Reset");
		assertThat(sent.getValue().getText()).isEqualTo(LINK);
	}

	@Test
	void anSmtpFailureIsLoggedNotThrown(CapturedOutput output)
	{
		JavaMailSender sender = mock(JavaMailSender.class);
		doThrow(new MailSendException("connection refused")).when(sender).send(any(SimpleMailMessage.class));

		new SmtpMailer(sender, "no-reply@salon.test").send("mia@glow.test", "Reset", LINK);

		assertThat(output).contains("Couldn't send \"Reset\" to m***@glow.test").doesNotContain("secret-token");
	}

	@Test
	void withoutSmtpTheEmailIsLoggedInDevelopment(CapturedOutput output)
	{
		new LogMailer(true).send("mia@glow.test", "Reset", LINK);

		assertThat(output).contains("mia@glow.test").contains(LINK);
	}

	@Test
	void withoutSmtpInProductionNothingAReaderCouldUseIsLogged(CapturedOutput output)
	{
		new LogMailer(false).send("mia@glow.test", "Reset", LINK);

		assertThat(output).contains("\"Reset\" to m***@glow.test not sent").doesNotContain("secret-token");
	}
}
