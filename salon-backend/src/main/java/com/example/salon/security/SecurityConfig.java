package com.example.salon.security;

import com.example.salon.dao.UserDao;
import com.example.salon.logging.CallerLoggingFilter;
import com.example.salon.logging.RequestLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig
{

	private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
	private final RestAccessDeniedHandler restAccessDeniedHandler;

	@Value("${app.cors.allowed-origins:http://localhost:3000}")
	private String allowedOrigins;

	@Value("${app.require-https:false}")
	private boolean requireHttps;

	public SecurityConfig(RestAuthenticationEntryPoint restAuthenticationEntryPoint,
			RestAccessDeniedHandler restAccessDeniedHandler)
	{
		this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
		this.restAccessDeniedHandler = restAccessDeniedHandler;
	}

	@Bean
	public PasswordEncoder passwordEncoder()
	{
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityContextRepository securityContextRepository(UserDao userDao)
	{
		return new RefreshingSecurityContextRepository(new HttpSessionSecurityContextRepository(), userDao);
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception
	{
		return configuration.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository) throws Exception
	{
		http
				.securityContext(sc -> sc.securityContextRepository(securityContextRepository))
				// Records who the caller is for the request log line (RequestLoggingFilter).
				.addFilterAfter(new CallerLoggingFilter(), SecurityContextHolderFilter.class)
				.cors(Customizer.withDefaults())
				// No CSRF token (BE-28). Another site can't use a signed-in user's session because:
				// - the browser sends its Origin with every POST, PUT and DELETE, including a form's or a
				//   fetch() that skips the preflight, and the CORS filter answers 403 to any Origin not in
				//   app.cors.allowed-origins before a controller runs (CrossOriginTest);
				// - the session cookie is SameSite=Lax (server.servlet.session.cookie.same-site), so a browser
				//   leaves it off another site's writes in the first place;
				// - no GET changes anything.
				// This holds while allowed-origins lists only origins this app's own frontends are served
				// from, and every one of them is trusted. Before adding a third-party origin, or a GET that
				// changes something, turn CSRF protection back on (CookieCsrfTokenRepository).
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/error").permitAll()
						// For a load balancer or orchestrator: UP or DOWN and nothing more (BE-39). No other
						// actuator endpoint is exposed, and anyRequest() below would deny it anyway.
						.requestMatchers(HttpMethod.GET, "/actuator/health/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
						// Forgetting your password means you can't sign in (FE-13).
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password")
								.permitAll()
						.requestMatchers("/api/v1/auth/**").authenticated()
						// Listing every user/address/contact leaks everyone's PII to any logged-in
						// customer - keep the bulk "list all" endpoints admin-only.
						.requestMatchers(HttpMethod.GET, "/api/v1/users", "/api/v1/addresses", "/api/v1/contacts")
								.hasAnyRole("ADMIN", "SUPER_ADMIN")
						// Single-record access below is "self or admin" - a URL matcher can't see who
						// owns a row, so these just require login and the controller/service enforces
						// ownership before returning or mutating the record.
						.requestMatchers(HttpMethod.GET, "/api/v1/users/**", "/api/v1/addresses/**", "/api/v1/contacts/**")
								.authenticated()
						.requestMatchers(HttpMethod.PUT, "/api/v1/users/**", "/api/v1/addresses/**", "/api/v1/contacts/**")
								.authenticated()
						.requestMatchers(HttpMethod.DELETE, "/api/v1/users/**", "/api/v1/addresses/**", "/api/v1/contacts/**")
								.authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/**").authenticated()
						.requestMatchers(HttpMethod.POST, "/api/v1/businesses").hasRole("SUPER_ADMIN")
						// Employees add customers too (front desk). CustomerService scopes it to their own salon.
						.requestMatchers(HttpMethod.POST, "/api/v1/businesses/*/customers").authenticated()
						// Employees book and change appointments too. AppointmentService scopes it to their own salon.
						.requestMatchers(HttpMethod.POST, "/api/v1/businesses/*/appointments").authenticated()
						.requestMatchers(HttpMethod.PUT, "/api/v1/businesses/*/appointments/**").authenticated()
						.requestMatchers(HttpMethod.POST, "/api/v1/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/v1/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
						.anyRequest().denyAll()
				)
				.exceptionHandling(eh -> eh
						.authenticationEntryPoint(restAuthenticationEntryPoint)
						.accessDeniedHandler(restAccessDeniedHandler)
				);
		if (requireHttps) {
			// Under the prod profile: nothing but the health check over plain HTTP (BE-25).
			http.addFilterBefore(new RequireHttpsFilter(), SecurityContextHolderFilter.class);
		}
		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource()
	{
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		// Only what the panel sends (BE-29): JSON, and a request id to match its error reports to the logs.
		configuration.setAllowedHeaders(List.of(HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, RequestLog.REQUEST_ID_HEADER));
		configuration.setAllowCredentials(true);
		// Fails startup, rather than every request, if allowed-origins is "*": any site could then use the session.
		configuration.validateAllowCredentials();
		// Lets the panel read the id to show it alongside an error, so a report can be matched to the logs,
		// and how long a locked-out sign-in has to wait (BE-22).
		configuration.setExposedHeaders(List.of(RequestLog.REQUEST_ID_HEADER, HttpHeaders.RETRY_AFTER));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
