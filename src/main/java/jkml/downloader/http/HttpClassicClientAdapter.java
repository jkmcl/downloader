package jkml.downloader.http;

import java.io.IOException;
import java.net.URI;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;

class HttpClassicClientAdapter implements HttpClientAdapter {

	private CloseableHttpClient client;

	@Override
	public void initialize(HttpClientConfig config) {
		var socketConfig = SocketConfig.custom()
				.setSoTimeout(HttpClientConfig.SOCKET_TIMEOUT)
				.build();

		var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
				.setDefaultSocketConfig(socketConfig)
				.setDefaultConnectionConfig(config.createConnectionConfig())
				.build();

		client = HttpClientBuilder.create()
				.disableAuthCaching()
				.disableConnectionState()
				.disableCookieManagement()
				.setConnectionManager(connectionManager)
				.setDefaultRequestConfig(config.createRequestConfig())
				.setDefaultHeaders(config.getDefaultHeaders())
				.setRedirectStrategy(CustomRedirectStrategy.INSTANCE)
				.setRetryStrategy(CustomRetryStrategy.INSTANCE)
				.setUserAgent(config.getUserAgent())
				.build();
	}

	@Override
	public void close() throws IOException {
		if (client != null) {
			client.close();
		}
	}

	@Override
	public ClassicHttpRequest createRequest(Method method, URI uri) {
		return new BasicClassicHttpRequest(method, uri);
	}

	@Override
	public <T> T execute(HttpRequest request, ResponseHandler<T> responseHandler) throws IOException {
		return client.execute((ClassicHttpRequest) request, responseHandler);
	}

}
