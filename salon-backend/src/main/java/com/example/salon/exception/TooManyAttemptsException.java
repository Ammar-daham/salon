package com.example.salon.exception;

import lombok.EqualsAndHashCode;

import java.time.Duration;

/** A 429 that says how long to wait, in the message and as Retry-After. */
@EqualsAndHashCode(callSuper = true)
public class TooManyAttemptsException extends BaseException
{
    private final long retryAfterSeconds;

    public TooManyAttemptsException(Duration retryAfter)
    {
        this(Math.max(1, (retryAfter.toMillis() + 999) / 1000));
    }

    private TooManyAttemptsException(long retryAfterSeconds)
    {
        super("Too many failed sign-in attempts. Try again in " + minutes(retryAfterSeconds) + ".",
                ErrorCode.TOO_MANY_REQUESTS);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds()
    {
        return retryAfterSeconds;
    }

    /** Rounded up, so "1 minute" never means a few seconds more. */
    private static String minutes(long seconds)
    {
        long minutes = (seconds + 59) / 60;
        return minutes == 1 ? "1 minute" : minutes + " minutes";
    }
}
