package jkml.downloader.http;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.hc.client5.http.protocol.RedirectStrategy;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.message.BasicHttpResponse;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.junit.jupiter.api.Test;

class CustomRedirectStrategyTests {

	private static final String SOURCE_URL = "https://source/";

	private static final String TARGET_URL = "https://target/";

	private static final String REFRESH_HEADER_NAME = "Refresh";

	private static final String REFRESH_HEADER_VALUE = "0;URL=" + TARGET_URL;

	private final RedirectStrategy strategy = CustomRedirectStrategy.INSTANCE;

	@Test
	void testIsRedirected_default() throws HttpException {
		var context = new BasicHttpContext();
		var request = new BasicHttpRequest(Method.GET, SOURCE_URL);
		var response = new BasicHttpResponse(HttpStatus.SC_MOVED_PERMANENTLY);
		response.setHeader(HttpHeaders.LOCATION, TARGET_URL);

		assertTrue(strategy.isRedirected(request, response, context));
	}

	@Test
	void testIsRedirected_custom() throws HttpException {
		var context = new BasicHttpContext();
		var request = new BasicHttpRequest(Method.GET, SOURCE_URL);

		var response1 = new BasicHttpResponse(HttpStatus.SC_OK);
		assertFalse(strategy.isRedirected(request, response1, context));

		var response2 = new BasicHttpResponse(HttpStatus.SC_BAD_REQUEST);
		response2.setHeader(REFRESH_HEADER_NAME, REFRESH_HEADER_VALUE);
		assertFalse(strategy.isRedirected(request, response2, context));

		var response3 = new BasicHttpResponse(HttpStatus.SC_OK);
		response3.setHeader(REFRESH_HEADER_NAME, REFRESH_HEADER_VALUE);
		assertTrue(strategy.isRedirected(request, response3, context));
	}

}
