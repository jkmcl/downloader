package jkml.downloader.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;

import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.message.BasicHttpResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LinkResponseHandlerTests {

	private static final Logger logger = LoggerFactory.getLogger(LinkResponseHandlerTests.class);

	@Test
	void testStart_invalidCode() throws IOException {
		var response = new BasicHttpResponse(HttpStatus.SC_OK);

		var handler = new LinkResponseHandler();
		try {
			handler.start(response, null);
			fail();
		} catch (ResponseException e) {
			logger.info("Exception message: {}", e.getMessage());
		} finally {
			handler.releaseResources();
		}
	}

	@Test
	void testStart_noLocation() throws IOException {
		var response = new BasicHttpResponse(HttpStatus.SC_MOVED_TEMPORARILY);

		var handler = new LinkResponseHandler();
		try {
			handler.start(response, null);
			fail();
		} catch (ResponseException e) {
			logger.info("Exception message: {}", e.getMessage());
		} finally {
			handler.releaseResources();
		}
	}

	@Test
	void testBuildResult() throws IOException {
		var link = URI.create("http://localhost/");
		var response = new BasicHttpResponse(HttpStatus.SC_MOVED_PERMANENTLY);
		response.setHeader(HttpHeaders.LOCATION, link.toString());

		var handler = new LinkResponseHandler();
		try {
			handler.start(response, null);
			handler.data(null, false);
			assertEquals(link, handler.buildResult());
		} finally {
			handler.releaseResources();
		}
	}

}
