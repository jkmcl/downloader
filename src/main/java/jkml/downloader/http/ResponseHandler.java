package jkml.downloader.http;

import org.apache.hc.client5.http.async.methods.AbstractBinResponseConsumer;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

abstract class ResponseHandler<T> extends AbstractBinResponseConsumer<T> implements HttpClientResponseHandler<T> {

	@Override
	protected int capacityIncrement() {
		return Integer.MAX_VALUE;
	}

}
