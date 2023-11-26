package jkml.downloader.http;

import java.io.IOException;

import org.apache.hc.client5.http.async.methods.AbstractBinResponseConsumer;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

abstract class ResponseHandler<T> extends AbstractBinResponseConsumer<T> implements HttpClientResponseHandler<T> {

	@Override
	protected int capacityIncrement() {
		return Integer.MAX_VALUE;
	}

	protected void checkCode(int code) throws IOException {
		if (code != HttpStatus.SC_OK) {
			throw new IOException("Unexpected HTTP response status code: " + code);
		}
	}

}
