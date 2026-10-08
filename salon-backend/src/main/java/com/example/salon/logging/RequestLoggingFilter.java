package com.example.salon.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Writes one line per request once the response is decided, for example:
 * <pre>
 * POST /api/v1/businesses/1/customers -> 201 in 34 ms (user=2 ADMIN, ip=10.0.0.5)
 * DELETE /api/v1/businesses/1 -> 409 in 12 ms (user=1 SUPER_ADMIN, ip=10.0.0.5) error=CONFLICT "Business with id 1 still has staff. ..."
 * </pre>
 * Bodies are never logged: requests carry passwords and responses carry customer PII. The status,
 * plus the error code and message the client was sent, say what happened without either.
 *
 * Runs before Spring Security, so it also sees the 401s and 403s the security filters write, and
 * every log line from inside the request carries the same request id.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter
{
	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

	// A caller-supplied id is reused only if it's short and plain, so it can't forge log lines.
	private static final Pattern SAFE_REQUEST_ID = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException
	{
		String requestId = requestIdFor(request);
		MDC.put(RequestLog.REQUEST_ID, requestId);
		response.setHeader(RequestLog.REQUEST_ID_HEADER, requestId);

		long start = System.nanoTime();
		boolean failed = true;
		try {
			chain.doFilter(request, response);
			failed = false;
		} finally {
			long millis = (System.nanoTime() - start) / 1_000_000;
			// An exception escaping the whole chain means the container answers with a 500.
			int status = failed ? 500 : response.getStatus();
			logRequest(request, status, millis);
			MDC.remove(RequestLog.REQUEST_ID);
		}
	}

	private static String requestIdFor(HttpServletRequest request)
	{
		String supplied = request.getHeader(RequestLog.REQUEST_ID_HEADER);
		return supplied != null && SAFE_REQUEST_ID.matcher(supplied).matches()
				? supplied
				: UUID.randomUUID().toString();
	}

	private static void logRequest(HttpServletRequest request, int status, long millis)
	{
		String caller = (String) request.getAttribute(RequestLog.CALLER_ATTRIBUTE);
		StringBuilder line = new StringBuilder()
				.append(request.getMethod()).append(' ').append(request.getRequestURI()).append(queryNames(request))
				.append(" -> ").append(status).append(" in ").append(millis).append(" ms")
				.append(" (user=").append(caller != null ? caller : "anonymous")
				.append(", ip=").append(request.getRemoteAddr()).append(')');

		Object errorCode = request.getAttribute(RequestLog.ERROR_CODE_ATTRIBUTE);
		if (errorCode != null) {
			line.append(" error=").append(errorCode)
					.append(" \"").append(request.getAttribute(RequestLog.ERROR_MESSAGE_ATTRIBUTE)).append('"');
		}

		if ("OPTIONS".equals(request.getMethod())) {
			log.debug("{}", line); // CORS preflights: one per real request, and never interesting.
		} else if (status >= 500) {
			log.error("{}", line);
		} else if (status >= 400) {
			log.warn("{}", line);
		} else if (request.getRequestURI().startsWith("/actuator/health")) {
			log.debug("{}", line); // A load balancer's probe, every few seconds; only a failing one is news.
		} else {
			log.info("{}", line);
		}
	}

	/** Parameter names only ("?city&service"): a value can be a search term or other personal data. */
	private static String queryNames(HttpServletRequest request)
	{
		if (request.getQueryString() == null) {
			return "";
		}
		return "?" + String.join("&", Collections.list(request.getParameterNames()));
	}
}
