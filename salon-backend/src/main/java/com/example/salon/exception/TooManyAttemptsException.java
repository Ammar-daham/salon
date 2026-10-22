package com.example.salon.exception;

import lombok.EqualsAndHashCode;

import java.time.Duration;

/** A 429 that says how long to wait, in the message and as Retry-After. */
@EqualsAndHashCode(callSuper = true)
public class TooManyAttemptsException extends BaseException
{
    private final long retryAfterSeconds;

    /** Too many failed sign-ins, as LoginThrottle says. */
    public TooManyAttemptsException(Duration retryAfter)
    {
        this("Too many failed sign-in attempts.", retryAfter);
    }

    /** what is a sentence saying what there were too many of; how long to wait is added to it. */
    public TooManyAttemptsException(String what, Duration retryAfter)
    {
        this(what, Math.max(1, (retryAfter.toMillis() + 999) / 1000));
    }

    private TooManyAttemptsException(String what, long retryAfterSeconds)
    {
        super(what + " Try again in " + minutes(retryAfterSeconds) + ".", ErrorCode.TOO_MANY_REQUESTS);
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
