package jkml.downloader.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpMessage;
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
		HttpMessage message = createResponse();
		var expected = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		HttpUtils.setTimeHeader(message, HttpHeaders.LAST_MODIFIED, expected);

		var value = message.getFirstHeader(HttpHeaders.LAST_MODIFIED).getValue();
		assertEquals(expected, DateUtils.parseStandardDate(value));
	}

	@Test
	void testGetTimeHeader() {
		var expected = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		HttpMessage message = createResponseWithHeader(HttpHeaders.LAST_MODIFIED, DateUtils.formatStandardDate(expected));

		var value = HttpUtils.getTimeHeader(message, HttpHeaders.LAST_MODIFIED);

		assertEquals(expected, value);
	}

	@Test
	void testGetTimeHeader_noValue() {
		HttpMessage message = createResponseWithHeader(HttpHeaders.LAST_MODIFIED, null);

		var value = HttpUtils.getTimeHeader(message, HttpHeaders.LAST_MODIFIED);

		assertNull(value);
	}

	@Test
	void testGetTimeHeader_emptyValue() {
		HttpMessage message = createResponseWithHeader(HttpHeaders.LAST_MODIFIED, StringUtils.EMPTY);

		var value = HttpUtils.getTimeHeader(message, HttpHeaders.LAST_MODIFIED);

		assertNull(value);
	}

	@Test
	void testGetTimeHeader_invalidValue() {
		HttpMessage message = createResponseWithHeader(HttpHeaders.LAST_MODIFIED, HttpHeaders.LAST_MODIFIED);

		var value = HttpUtils.getTimeHeader(message, HttpHeaders.LAST_MODIFIED);

		assertNull(value);
	}

	@Test
	void testGetHeader_noHeader() {
		HttpMessage message = createResponse();

		var value = HttpUtils.getHeader(message, HttpHeaders.CONTENT_ENCODING);

		assertNull(value);
	}

	@Test
	void testGetHeader_noValue() {
		HttpMessage message = createResponseWithHeader(HttpHeaders.CONTENT_ENCODING, null);

		var value = HttpUtils.getHeader(message, HttpHeaders.CONTENT_ENCODING);

		assertNull(value);
	}

	@Test
	void testGetHeader() {
		var expected = "gzip";
		HttpMessage message = createResponseWithHeader(HttpHeaders.CONTENT_ENCODING, expected);

		var value = HttpUtils.getHeader(message, HttpHeaders.CONTENT_ENCODING);

		assertEquals(expected, value);
	}

	@Test
	void testGetFirstParameter() {
		var expected = "archive.zip";
		var response = createResponseWithHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + expected + "\"");

		var actual = HttpUtils.getFirstParameter(response, HttpHeaders.CONTENT_DISPOSITION, "filename");

		assertEquals(expected, actual);
	}

	@Test
	void testGetFirstParameter_noHeader() {
		var response = createResponse();

		var value = HttpUtils.getFirstParameter(response, HttpHeaders.CONTENT_DISPOSITION, "filename");

		assertNull(value);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "attachment", "attachment;", "attachment; filename" })
	void testGetFirstParameter_invalid(String arg) {
		var response = createResponseWithHeader(HttpHeaders.CONTENT_DISPOSITION, arg);

		var value = HttpUtils.getFirstParameter(response, HttpHeaders.CONTENT_DISPOSITION, "filename");

		assertNull(value);
	}

	@Test
	void testGetUri() {
		var expected = URI.create("https://localhost/");
		var request = new BasicHttpRequest(Method.GET, expected);
		assertEquals(expected, HttpUtils.getUri(request));
	}

}
