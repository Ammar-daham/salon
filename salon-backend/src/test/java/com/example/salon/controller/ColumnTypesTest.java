package com.example.salon.controller;

import com.example.salon.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** DB-07 coordinates, DB-08 money and currency, as the API sees them. */
class ColumnTypesTest extends IntegrationTest
{
	private static String service(String price)
	{
		return """
				{"name": "Gloss", "description": "x", "duration_minutes": 20, "price": %s, "is_active": true}
				""".formatted(price);
	}

	private static String address(String coordinates)
	{
		return """
				{"street": "12 Rosenthaler Str.", "city": "Berlin", "country": "Germany", "postal_code": "10119"%s}
				""".formatted(coordinates);
	}

	@Test
	void pricesAreExactDecimalsNotFloatingPoint() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);

		// 0.10 + 0.20 as doubles is 0.30000000000000004; as BigDecimal it is 0.30, and the scale survives.
		mvc.perform(post("/api/v1/businesses/" + Fixture.GLOW + "/services").session(admin)
						.contentType(MediaType.APPLICATION_JSON).content(service("0.30")))
				.andExpect(status().isCreated())
				.andExpect(content().string(containsString("\"price\":0.30")));

		mvc.perform(get("/api/v1/businesses/" + Fixture.GLOW).session(admin))
				.andExpect(content().string(containsString("\"price\":45.00")));
	}

	@Test
	void aPriceNeedsAtMostTwoDecimalsAndCannotBeOmitted() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		String services = "/api/v1/businesses/" + Fixture.GLOW + "/services";

		mvc.perform(post(services).session(admin).contentType(MediaType.APPLICATION_JSON).content(service("10.005")))
				.andExpect(status().isBadRequest());
		// A missing price used to bind to a double's default of 0, i.e. a silently free service.
		mvc.perform(post(services).session(admin).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Gloss", "duration_minutes": 20, "is_active": true}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void salonsArePricedInEurosUnlessToldOtherwise() throws Exception
	{
		MockHttpSession superAdmin = loginAs(Fixture.SUPER_ADMIN);

		mvc.perform(get("/api/v1/businesses/" + Fixture.GLOW).session(superAdmin))
				.andExpect(jsonPath("$.currency").value("EUR"));

		mvc.perform(post("/api/v1/businesses").session(superAdmin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Nordic Nails", "image": "x", "currency": "SEK"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.currency").value("SEK"));

		mvc.perform(post("/api/v1/businesses").session(superAdmin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Bad Currency", "image": "x", "currency": "euro"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void updatingABusinessKeepsItsCurrencyUnlessOneIsSent() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		String glow = "/api/v1/businesses/" + Fixture.GLOW;

		mvc.perform(put(glow).session(admin).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Glow Beauty Studio", "description": "Colour and precision cuts."}
								"""))
				.andExpect(status().isOk());
		mvc.perform(get(glow).session(admin)).andExpect(jsonPath("$.currency").value("EUR"));

		mvc.perform(put(glow).session(admin).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Glow Beauty Studio", "description": "Colour and precision cuts.", "currency": "SEK"}
								"""))
				.andExpect(status().isOk());
		mvc.perform(get(glow).session(admin)).andExpect(jsonPath("$.currency").value("SEK"));
	}

	@Test
	void coordinatesAreNumbersRoundedToSixDecimals() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		String glowAddress = "/api/v1/addresses/" + Fixture.GLOW_ADDRESS;

		mvc.perform(put(glowAddress).session(admin).contentType(MediaType.APPLICATION_JSON)
						.content(address(", \"latitude\": 52.52918757, \"longitude\": 13.4014471")))
				.andExpect(status().isOk());

		mvc.perform(get(glowAddress).session(admin))
				.andExpect(content().string(containsString("\"latitude\":52.529188")))
				.andExpect(content().string(containsString("\"longitude\":13.401447")));
	}

	@Test
	void coordinatesMustBeInRangeAndComeAsAPair() throws Exception
	{
		MockHttpSession admin = loginAs(Fixture.GLOW_ADMIN);
		String glowAddress = "/api/v1/addresses/" + Fixture.GLOW_ADDRESS;

		mvc.perform(put(glowAddress).session(admin).contentType(MediaType.APPLICATION_JSON)
						.content(address(", \"latitude\": 91, \"longitude\": 13.4")))
				.andExpect(status().isBadRequest());
		mvc.perform(put(glowAddress).session(admin).contentType(MediaType.APPLICATION_JSON)
						.content(address(", \"latitude\": 52.5")))
				.andExpect(status().isBadRequest());
	}
}
