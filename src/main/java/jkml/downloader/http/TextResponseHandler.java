package jkml.downloader.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.util.ByteArrayBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Based on org.apache.hc.client5.http.async.methods.SimpleAsyncEntityConsumer
 */
class TextResponseHandler extends ResponseHandler<TextResult> {

	private final Logger logger = LoggerFactory.getLogger(TextResponseHandler.class);

	private Charset charset = StandardCharsets.UTF_8;

	private ByteArrayBuffer buffer;

	private String text;

	@Override
	protected void doStart(HttpResponse response, ContentType contentType) throws IOException {
		if (contentType != null) {
			charset = contentType.getCharset(charset);
		}

		buffer = new ByteArrayBuffer(8192);
	}

	@Override
	protected void data(ByteBuffer src, boolean endOfStream) throws IOException {
		buffer.append(src);

		if (endOfStream) {
			var bytes = buffer.toByteArray();
			logger.info("Response content length: {}", bytes.length);
			text = new String(bytes, charset);
		}
	}

	@Override
	protected TextResult buildResult() {
		return new TextResult(text);
	}

	@Override
	public void releaseResources() {
		// No resource requires release
	}

}
