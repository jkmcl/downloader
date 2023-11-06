package jkml.downloader.http;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;

import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.concurrent.DefaultThreadFactory;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.nio.support.BasicRequestProducer;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.reactor.IOReactorConfig;

class HttpAsyncClientAdapter implements HttpClientAdapter {

	private CloseableHttpAsyncClient client;

	@Override
	public void initialize(HttpClientConfig config) {
		var tlsConfig = TlsConfig.custom()
				.setVersionPolicy(HttpVersionPolicy.NEGOTIATE)
				.build();

		var connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
				.setDefaultTlsConfig(tlsConfig)
				.setDefaultConnectionConfig(config.createConnectionConfig())
				.build();

		var ioReactorConfig = IOReactorConfig.custom()
				.setIoThreadCount(Math.max(Runtime.getRuntime().availableProcessors(), 8))
                .setSoTimeout(HttpClientConfig.SOCKET_TIMEOUT)
                .build();

		client = HttpAsyncClientBuilder.create()
				.disableAuthCaching()
				.disableConnectionState()
				.disableCookieManagement()
				.setConnectionManager(connectionManager)
				.setThreadFactory(new DefaultThreadFactory("http", true))
				.setIOReactorConfig(ioReactorConfig)
				.setDefaultRequestConfig(config.createRequestConfig())
				.setDefaultHeaders(config.getDefaultHeaders())
				.setRedirectStrategy(CustomRedirectStrategy.INSTANCE)
				.setUserAgent(config.getUserAgent())
				.build();

		client.start();
	}

	@Override
	public void close() throws IOException {
		if (client != null) {
			client.close();
		}
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
