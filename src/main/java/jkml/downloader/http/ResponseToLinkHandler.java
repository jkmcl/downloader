package jkml.downloader.http;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ResponseToLinkHandler extends ResponseHandler<LinkResult> {

	private final Logger logger = LoggerFactory.getLogger(ResponseToLinkHandler.class);

	private URI location;

	@Override
	protected boolean isValid(int code) {
		return (code == HttpStatus.SC_MOVED_PERMANENTLY || code == HttpStatus.SC_MOVED_TEMPORARILY
				|| code == HttpStatus.SC_SEE_OTHER || code == HttpStatus.SC_TEMPORARY_REDIRECT
				|| code == HttpStatus.SC_PERMANENT_REDIRECT);
	}

	@Override
	protected void doStart(HttpResponse response, ContentType contentType) throws HttpException, IOException {
		var header = response.getFirstHeader(HttpHeaders.LOCATION);
		if (header == null) {
			throw new IOException("Location header not found in response");
		}
		location = URI.create(header.getValue());
	}

	@Override
	protected void data(ByteBuffer src, boolean endOfStream) throws IOException {
		// Skip data processing
	}

	@Override
	protected LinkResult buildResult() {
		logger.info("Response location: {}", location);
		return new LinkResult(location);
	}

	@Override
	public void releaseResources() {
		// No resource requires release
	}

}
