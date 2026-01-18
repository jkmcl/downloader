package jkml.downloader.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.util.ByteArrayBuffer;

/**
 * Based on org.apache.hc.client5.http.async.methods.SimpleAsyncEntityConsumer
 */
class TextResponseHandler extends ResponseHandler<String> {

	private Charset charset = StandardCharsets.UTF_8;

	private ByteArrayBuffer buffer;

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
	}

	@Override
	protected String buildResult() {
		return new String(buffer.toByteArray(), charset);
	}

	@Override
	public void releaseResources() {
		// No resource requires release
	}

}
