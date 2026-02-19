package jkml.downloader.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.message.BasicHttpResponse;
import org.junit.jupiter.api.Test;

class TextResponseHandlerTests {

	@Test
	void testBuildResult_noHeader() throws IOException {
		var text = "hello";
		var response = new BasicHttpResponse(HttpStatus.SC_OK);
		var buffer = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));

		var handler = new TextResponseHandler();
		try {
			handler.start(response, null);
			handler.data(buffer, true);
			assertEquals(text, handler.buildResult());
		} finally {
			handler.releaseResources();
		}
	}

	@Test
	void testBuildResult() throws IOException {
		var text = "hello";
		var response = new BasicHttpResponse(HttpStatus.SC_OK);
		response.addHeader(HttpHeaders.CONTENT_LENGTH, "1");
		var contentType = ContentType.TEXT_HTML.withCharset(StandardCharsets.UTF_8);
		var buffer = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));

		var handler = new TextResponseHandler();
		try {
			handler.start(response, contentType);
			handler.data(buffer, true);
			assertEquals(text, handler.buildResult());
		} finally {
			handler.releaseResources();
		}
	}

}
