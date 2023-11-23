package jkml.downloader.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.ByteArrayBuffer;

/**
 * Based on org.apache.hc.client5.http.async.methods.SimpleAsyncEntityConsumer
 */
class ResponseToTextHandler extends ResponseHandler<String> {

	private Charset charset = StandardCharsets.UTF_8;

	private ByteArrayBuffer buffer;

	private void preprocess(HttpResponse response, ContentType contentType) {
		var code = response.getCode();
		if (code != HttpStatus.SC_OK) {
			throw new HttpStatusException(code);
		}
		if (contentType != null) {
			charset = contentType.getCharset(charset);
		}
	}

	@Override
	public String handleResponse(ClassicHttpResponse response) throws HttpException, IOException {
		preprocess(response, null);
		return EntityUtils.toString(response.getEntity(), charset);
	}

	@Override
	protected void start(HttpResponse response, ContentType contentType) throws HttpException, IOException {
		preprocess(response, contentType);
		buffer = new ByteArrayBuffer(8192);
	}

	@Override
	protected void data(ByteBuffer src, boolean endOfStream) throws IOException {
		if (src.hasArray()) {
			buffer.append(src.array(), src.arrayOffset() + src.position(), src.remaining());
		} else {
			var remaining = src.remaining();
			while (remaining-- > 0) {
				buffer.append(src.get());
			}
		}
	}

	@Override
	protected String buildResult() {
		return new String(buffer.toByteArray(), charset);
	}

	@Override
	public void releaseResources() {
		buffer = null;
	}

}
