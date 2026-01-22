package jkml.downloader.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.util.ByteArrayBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Based on org.apache.hc.client5.http.async.methods.SimpleAsyncEntityConsumer
 */
class TextResponseHandler extends ResponseHandler<String> {

	private final Logger logger = LoggerFactory.getLogger(TextResponseHandler.class);

	private Charset charset = StandardCharsets.UTF_8;

	private ByteArrayBuffer buffer;

	@Override
	protected void doStart(HttpResponse response, ContentType contentType) throws IOException {
		if (contentType != null) {
			charset = contentType.getCharset(charset);
		}
		var header = HttpUtils.getHeader(response, HttpHeaders.CONTENT_LENGTH);
		var capacity = (header == null) ? 65536 : Integer.parseInt(header);
		buffer = new ByteArrayBuffer(capacity);
	}

	@Override
	protected void data(ByteBuffer src, boolean endOfStream) throws IOException {
		buffer.append(src);
	}

	@Override
	protected String buildResult() {
		logger.info("Content length: {}", buffer.length());
		return new String(buffer.toByteArray(), charset);
	}

	@Override
	public void releaseResources() {
		// No resource requires release
	}

}
