package com.example.salon.support;

import com.example.salon.mail.Mailer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Keeps every email instead of sending it, so a test can read what was sent, e.g. a reset link. */
public class RecordingMailer implements Mailer
{
	public record Mail(String to, String subject, String text)
	{
	}

	private final List<Mail> sent = new CopyOnWriteArrayList<>();

	@Override
	public void send(String to, String subject, String text)
	{
		sent.add(new Mail(to, subject, text));
	}

	public List<Mail> sent()
	{
		return List.copyOf(sent);
	}

	public void clear()
	{
		sent.clear();
	}

	/**
	 * Records emails, and runs background work on the calling thread, so it's finished by the time the
	 * request that started it has its answer.
	 */
	@TestConfiguration
	public static class Config
	{
		@Bean
		@Primary
		public RecordingMailer recordingMailer()
		{
			return new RecordingMailer();
		}

		@Bean
		@Primary
		public TaskExecutor syncTaskExecutor()
		{
			return new SyncTaskExecutor();
		}
	}
}
