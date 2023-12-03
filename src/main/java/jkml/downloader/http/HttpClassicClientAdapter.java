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

	private final CloseableHttpClient client;

	public HttpClassicClientAdapter(HttpClientConfig config) {
		var socketConfig = SocketConfig.custom()
				.setSoTimeout(HttpClientConfig.TIMEOUT)
				.build();

		var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
				.setDefaultConnectionConfig(config.getConnectionConfig())
				.setDefaultTlsConfig(config.getTlsConfig())
				.setDefaultSocketConfig(socketConfig)
				.build();

		client = HttpClientBuilder.create()
				.disableAuthCaching()
				.disableAutomaticRetries()
				.disableConnectionState()
				.disableCookieManagement()
				.setConnectionManager(connectionManager)
				.setDefaultHeaders(config.getDefaultHeaders())
				.setDefaultRequestConfig(config.getRequestConfig())
				.setRedirectStrategy(CustomRedirectStrategy.INSTANCE)
				.setUserAgent(config.getUserAgent())
				.build();
	}

	@Override
	public void close() throws IOException {
		client.close();
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
