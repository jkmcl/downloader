package jkml.downloader.http;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;

import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.concurrent.DefaultThreadFactory;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.nio.support.BasicRequestProducer;
import org.apache.hc.core5.reactor.IOReactorConfig;

class HttpAsyncClientAdapter implements HttpClientAdapter {

	private final CloseableHttpAsyncClient client;

	public HttpAsyncClientAdapter(HttpClientConfig config) {
		var ioReactorConfig = IOReactorConfig.custom()
				.setSoTimeout(HttpClientConfig.TIMEOUT)
				.setIoThreadCount(Math.min(Runtime.getRuntime().availableProcessors(), 8))
				.build();

		var connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
				.setDefaultConnectionConfig(config.getConnectionConfig())
				.setDefaultTlsConfig(config.getTlsConfig())
				.build();

		client = HttpAsyncClientBuilder.create()
				.disableAuthCaching()
				.disableAutomaticRetries()
				.disableConnectionState()
				.disableCookieManagement()
				.setConnectionManager(connectionManager)
				.setDefaultHeaders(config.getDefaultHeaders())
				.setDefaultRequestConfig(config.getRequestConfig())
				.setRedirectStrategy(CustomRedirectStrategy.INSTANCE)
				.setUserAgent(config.getUserAgent())
				.setIOReactorConfig(ioReactorConfig)
				.setThreadFactory(new DefaultThreadFactory("http", true))
				.build();

		client.start();
	}

	@Override
	public void close() throws IOException {
		client.close();
	}

	@Override
	public HttpRequest createRequest(Method method, URI uri) {
		return new BasicHttpRequest(method, uri);
	}

	@Override
	public <T> T execute(HttpRequest request, ResponseHandler<T> responseHandler) throws ExecutionException, InterruptedException {
		return client.execute(new BasicRequestProducer(request, null), responseHandler, null).get();
	}

}
