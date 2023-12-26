package jkml.downloader.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.util.ByteArrayBuffer;

/**
 * Based on org.apache.hc.client5.http.async.methods.SimpleAsyncEntityConsumer
 */
class ResponseToTextHandler extends ResponseHandler<TextResult> {

	private Charset charset = StandardCharsets.UTF_8;

	private ByteArrayBuffer buffer;

	@Override
	protected void start(HttpResponse response, ContentType contentType) throws HttpException, IOException {
		checkCode(response.getCode());
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
	protected TextResult buildResult() {
		return new TextResult(Status.OK, new String(buffer.toByteArray(), charset));
	}

	@Override
	public void releaseResources() {
		// No resource requires release
	}

}
