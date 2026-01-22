package jkml.downloader.http;

import java.io.IOException;

import org.apache.hc.client5.http.async.methods.AbstractBinResponseConsumer;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;

abstract class ResponseHandler<T> extends AbstractBinResponseConsumer<T> {

	@Override
	protected final int capacityIncrement() {
		return Integer.MAX_VALUE;
	}

	@Override
	protected final void start(HttpResponse response, ContentType contentType) throws IOException {
		if (!isValid(response.getCode())) {
			throw new ResponseException("Unexpected status code: " + response.getCode());
		}
		doStart(response, contentType);
	}

	protected boolean isValid(int code) {
		return code == HttpStatus.SC_OK;
	}

	protected abstract void doStart(HttpResponse response, ContentType contentType) throws IOException;

}
