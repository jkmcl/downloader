package jkml.downloader.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.message.BasicHttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jkml.downloader.util.StringUtils;

class HttpUtilsTests {

	private static HttpResponse createResponse() {
		return new BasicHttpResponse(HttpStatus.SC_OK);
	}

	private static HttpResponse createResponseWithHeader(String name, String value) {
		var response = createResponse();
		response.setHeader(name, value);
		return response;
	}

	@Test
	void testSetTimeHeader() {
		var expected = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		var message = createResponse();

		HttpUtils.setTimeHeader(message, HttpHeaders.DATE, expected);

		var value = message.getFirstHeader(HttpHeaders.DATE).getValue();
		assertEquals(expected, DateUtils.parseStandardDate(value));
	}

	@Test
	void testGetTimeHeader() {
		var expected = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		var message = createResponseWithHeader(HttpHeaders.DATE, DateUtils.formatStandardDate(expected));

		var value = HttpUtils.getTimeHeader(message, HttpHeaders.DATE);

		assertEquals(expected, value);
	}

	@Test
	void testGetTimeHeader_noValue() {
		var message = createResponseWithHeader(HttpHeaders.DATE, null);

		var value = HttpUtils.getTimeHeader(message, HttpHeaders.DATE);

		assertNull(value);
	}

	@Test
	void testGetTimeHeader_emptyValue() {
		var message = createResponseWithHeader(HttpHeaders.DATE, StringUtils.EMPTY);

		var value = HttpUtils.getTimeHeader(message, HttpHeaders.DATE);

		assertNull(value);
	}

	@Test
	void testGetTimeHeader_invalidValue() {
		var message = createResponseWithHeader(HttpHeaders.DATE, "Not a date");

		var value = HttpUtils.getTimeHeader(message, HttpHeaders.DATE);

		assertNull(value);
	}

	@Test
	void testGetHeader_noHeader() {
		var message = createResponse();

		var value = HttpUtils.getHeader(message, HttpHeaders.SERVER);

		assertNull(value);
	}

	@Test
	void testGetHeader_noValue() {
		var message = createResponseWithHeader(HttpHeaders.SERVER, null);

		var value = HttpUtils.getHeader(message, HttpHeaders.SERVER);

		assertNull(value);
	}

	@Test
	void testGetHeader() {
		var expected = "MyServer";
		var message = createResponseWithHeader(HttpHeaders.SERVER, expected);

		var value = HttpUtils.getHeader(message, HttpHeaders.SERVER);

		assertEquals(expected, value);
	}

	@Test
	void testGetParameter() {
		var expected = "archive.zip";
		var response = createResponseWithHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + expected + "\"");

		var actual = HttpUtils.getParameter(response, HttpHeaders.CONTENT_DISPOSITION, "filename");

		assertEquals(expected, actual);
	}

	@Test
	void testGetParameter_noHeader() {
		var response = createResponse();

		var value = HttpUtils.getParameter(response, HttpHeaders.CONTENT_DISPOSITION, "filename");

		assertNull(value);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "attachment", "attachment;", "attachment; filename" })
	void testGetParameter_invalid(String arg) {
		var response = createResponseWithHeader(HttpHeaders.CONTENT_DISPOSITION, arg);

		var value = HttpUtils.getParameter(response, HttpHeaders.CONTENT_DISPOSITION, "filename");

		assertNull(value);
	}

	@Test
	void testGetUri() {
		var expected = URI.create("https://localhost/");
		var request = new BasicHttpRequest(Method.GET, expected);
		assertEquals(expected, HttpUtils.getUri(request));
	}

	@Test
	void testGetUri_throws() throws Exception {
		var request = mock(HttpRequest.class);
		when(request.getUri()).thenThrow(new URISyntaxException(StringUtils.EMPTY, StringUtils.EMPTY));
		assertThrows(IllegalArgumentException.class, () -> HttpUtils.getUri(request));
	}

}
