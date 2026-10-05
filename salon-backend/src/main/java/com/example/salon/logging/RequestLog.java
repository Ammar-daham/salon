package com.example.salon.logging;

import jakarta.servlet.http.HttpServletRequest;

/**
 * What the request log line needs to know but the filter can't see: who the caller was (resolved
 * inside the security chain) and what error the client was sent (built by the exception handler
 * or the security entry points). Each piece is parked on the request as an attribute.
 */
public final class RequestLog
{
	/** MDC key; printed on every log line the request produces, and returned as X-Request-Id. */
	public static final String REQUEST_ID = "requestId";
	public static final String REQUEST_ID_HEADER = "X-Request-Id";

	static final String CALLER_ATTRIBUTE = RequestLog.class.getName() + ".caller";
	static final String ERROR_CODE_ATTRIBUTE = RequestLog.class.getName() + ".errorCode";
	static final String ERROR_MESSAGE_ATTRIBUTE = RequestLog.class.getName() + ".errorMessage";

	private RequestLog()
	{
	}

	/** Called wherever an error response body is written, so the log line can say what was sent. */
	public static void recordError(HttpServletRequest request, String errorCode, String message)
	{
		request.setAttribute(ERROR_CODE_ATTRIBUTE, errorCode);
		request.setAttribute(ERROR_MESSAGE_ATTRIBUTE, message);
	}

	/** "a***@glow.test": enough to spot attacks on one account without logging the address itself. */
	public static String maskEmail(String email)
	{
		if (email == null || email.isBlank()) {
			return "<none>";
		}
		int at = email.indexOf('@');
		if (at <= 0) {
			return "***";
		}
		return email.charAt(0) + "***" + email.substring(at);
	}
}
