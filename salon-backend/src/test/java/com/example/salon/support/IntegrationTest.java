package com.example.salon.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack test base: the real Spring context, the real Flyway migrations and a real Postgres.
 * The container is started once and shared by every test class; the fixture in
 * fixtures/seed.sql is reloaded before each test so tests can't leak state into each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = {"/fixtures/reset.sql", "/fixtures/seed.sql"})
public abstract class IntegrationTest
{
	private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

	static 
	{
		POSTGRES.start();
	}

	@DynamicPropertySource
	static void datasource(DynamicPropertyRegistry registry)
	{
		// PostgresDatasource binds app.datasource.*, not spring.datasource.*, so @ServiceConnection can't be used.
		registry.add("app.datasource.jdbc-url", POSTGRES::getJdbcUrl);
		registry.add("app.datasource.username", POSTGRES::getUsername);
		registry.add("app.datasource.password", POSTGRES::getPassword);
	}

	@Autowired
	protected MockMvc mvc;

	/** Ids and credentials from fixtures/seed.sql. */
	public static final class Fixture
	{
		public static final String PASSWORD = "Password123!";

		public static final String SUPER_ADMIN = "superadmin@salon.test";
		public static final String GLOW_ADMIN = "anna.admin@glow.test";
		public static final String URBAN_ADMIN = "ben.admin@urban.test";
		public static final String GLOW_EMPLOYEE = "mia.stylist@glow.test";
		public static final String URBAN_EMPLOYEE = "leo.barber@urban.test";

		public static final long GLOW_ADMIN_ID = 2;
		public static final long URBAN_ADMIN_ID = 3;
		public static final long GLOW_EMPLOYEE_ID = 4;

		public static final long GLOW = 1;
		public static final long URBAN = 2;
		public static final long SERENITY_PENDING = 3;

		public static final long GLOW_ADDRESS = 1;
		public static final long GLOW_EMPLOYEE_PERSONAL_ADDRESS = 2;
		public static final long GLOW_CONTACT = 1;
		public static final long URBAN_CONTACT = 2;
		public static final long GLOW_EMPLOYEE_PERSONAL_CONTACT = 3;

		public static final long GLOW_HAIRCUT = 1;
		public static final long GLOW_MANICURE = 2;
		public static final long URBAN_FADE = 3;

		private Fixture()
		{
		}
	}

	/** Signs in through the real login endpoint and returns the resulting session. */
	protected MockHttpSession loginAs(String email) throws Exception
	{
		MvcResult result = mvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody(email, Fixture.PASSWORD)))
				.andExpect(status().isOk())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	protected static String loginBody(String email, String password)
	{
		return """
				{"email": "%s", "password": "%s"}
				""".formatted(email, password);
	}
}
