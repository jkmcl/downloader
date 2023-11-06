package jkml.downloader.http;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;

import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.Method;

interface HttpClientAdapter extends Closeable {

	void initialize(HttpClientConfig config);

	HttpRequest createRequest(Method method, URI uri);

	<T> T execute(HttpRequest request, ResponseHandler<T> responseHandler) throws IOException, ExecutionException, InterruptedException;

}
