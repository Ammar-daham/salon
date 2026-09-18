package com.example.salon.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.Instant;

// Builds the JSON body by hand rather than via an injected ObjectMapper: this app's Jackson
// autoconfiguration doesn't expose an ObjectMapper bean (see the split `spring-boot-starter-webmvc`
// dependency), and security-filter code runs independent of MVC's own message converters.
final class SecurityResponseWriter
{
	private SecurityResponseWriter()
	{
	}

	static void writeError(HttpServletResponse response, HttpServletRequest request,
			HttpStatus status, String errorCode, String message) throws IOException
	{
		String body = """
				{"errorCode":"%s","message":"%s","status":"%s","timestamp":"%s","path":"%s"}\
				""".formatted(
				escape(errorCode), escape(message), status.name(), Instant.now(), escape(request.getRequestURI())
		);

		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write(body);
	}

	private static String escape(String value)
	{
		return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
