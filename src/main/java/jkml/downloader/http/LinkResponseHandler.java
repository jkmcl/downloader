package jkml.downloader.http;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LinkResponseHandler extends ResponseHandler<URI> {

	private final Logger logger = LoggerFactory.getLogger(LinkResponseHandler.class);

	private URI location;

	@Override
	protected boolean isValid(int code) {
		return code == HttpStatus.SC_MOVED_PERMANENTLY || code == HttpStatus.SC_MOVED_TEMPORARILY
				|| code == HttpStatus.SC_SEE_OTHER || code == HttpStatus.SC_TEMPORARY_REDIRECT
				|| code == HttpStatus.SC_PERMANENT_REDIRECT;
	}

	@Override
	protected void doStart(HttpResponse response, ContentType contentType) throws IOException {
		var header = HttpUtils.getHeader(response, HttpHeaders.LOCATION);
		if (header == null) {
			throw new ResponseException(HttpHeaders.LOCATION + " header not found");
		}
		location = URI.create(header);
	}

	@Override
	protected void data(ByteBuffer src, boolean endOfStream) throws IOException {
		// Skip data processing
	}

	@Override
	protected URI buildResult() {
		logger.info("Location: {}", location);
		return location;
	}

	@Override
	public void releaseResources() {
		// No resource requires release
	}

}
