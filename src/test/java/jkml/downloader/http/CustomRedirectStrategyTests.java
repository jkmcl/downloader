package jkml.downloader.http;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.protocol.RedirectStrategy;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.junit.jupiter.api.Test;

class CustomRedirectStrategyTests {

	private static final String SOURCE_URL = "https://source/";

	private static final String TARGET_URL = "https://target/";

	private static final String REFRESH_HEADER_NAME = "Refresh";

	private static final String REFRESH_HEADER_VALUE = "0;URL=" + TARGET_URL;

	private final RedirectStrategy strategy = CustomRedirectStrategy.INSTANCE;

	@Test
	void testIsRedirected_default() throws HttpException {
		HttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_MOVED_PERMANENTLY);
		response.setHeader(HttpHeaders.LOCATION, TARGET_URL);

		assertTrue(strategy.isRedirected(new HttpGet(SOURCE_URL), response, new BasicHttpContext()));
	}

	@Test
	void testIsRedirected_custom() throws HttpException {
		HttpRequest request = new HttpGet(SOURCE_URL);
		HttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_OK);
		HttpContext context = new BasicHttpContext();

		assertFalse(strategy.isRedirected(request, response, context));

		response.setHeader(REFRESH_HEADER_NAME, REFRESH_HEADER_VALUE);
		assertTrue(strategy.isRedirected(request, response, context));
	}

	@Test
	void testGetLocationURI_default() throws HttpException {
		HttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_MOVED_PERMANENTLY);
		response.setHeader(HttpHeaders.LOCATION, TARGET_URL);

		assertNotNull(strategy.getLocationURI(new HttpGet(SOURCE_URL), response, new BasicHttpContext()));
	}

	@Test
	void testGetLocationURI_custom() throws HttpException {
		HttpRequest request = new HttpGet(SOURCE_URL);
		HttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_OK);
		HttpContext context = new BasicHttpContext();
		response.setHeader(REFRESH_HEADER_NAME, REFRESH_HEADER_VALUE);

		assertTrue(strategy.isRedirected(request, response, context));
		assertNotNull(strategy.getLocationURI(request, response, context));
	}

}
