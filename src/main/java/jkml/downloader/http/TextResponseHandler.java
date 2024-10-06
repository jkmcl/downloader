package jkml.downloader.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
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

	private ContentEncoding contentEncoding = null;

	private ByteArrayBuffer buffer;

	@Override
	protected void doStart(HttpResponse response, ContentType contentType) throws HttpException, IOException {
		if (contentType != null) {
			charset = contentType.getCharset(charset);
		}
		var value = HttpUtils.getHeader(response, HttpHeaders.CONTENT_ENCODING);
		if (value != null) {
			try {
				contentEncoding = ContentEncoding.fromString(value.strip());
				logger.info("Response content encoding: {}", value);
			} catch (IllegalArgumentException e) {
				throw new IOException("Unsupported response content encoding: " + value);
			}
		}
		buffer = new ByteArrayBuffer(8192);
	}

	@Override
	protected void data(ByteBuffer src, boolean endOfStream) throws IOException {
		buffer.append(src);
	}

	@Override
	protected TextResult buildResult() {
		var bytes = buffer.toByteArray();
		if (contentEncoding == null) {
			logger.info("Response content length: {}", bytes.length);
		} else {
			bytes = contentEncoding.decode(bytes);
			logger.info("Decoded response content length: {}", bytes.length);
		}
		return new TextResult(new String(bytes, charset));
	}

	@Override
	public void releaseResources() {
		// No resource requires release
	}

}
