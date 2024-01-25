package jkml.downloader.http;

import java.util.List;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.concurrent.DefaultThreadFactory;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

class HttpClientBuilder {

	private static final Timeout TIMEOUT = Timeout.ofSeconds(30);

	private List<Header> defaultHeaders;

	public HttpClientBuilder defaultHeaders(List<Header> value) {
		defaultHeaders = value;
		return this;
	}

	public CloseableHttpAsyncClient build() {
		var connectionConfig = ConnectionConfig.custom()
				.setConnectTimeout(TIMEOUT)
				.setSocketTimeout(TIMEOUT)
				.setTimeToLive(TimeValue.ofMinutes(1))
				.setValidateAfterInactivity(TimeValue.ZERO_MILLISECONDS)
				.build();

		var tlsConfig = TlsConfig.custom()
				.setHandshakeTimeout(TIMEOUT)
				.setVersionPolicy(HttpVersionPolicy.NEGOTIATE)
				.build();

		var connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
				.setDefaultConnectionConfig(connectionConfig)
				.setDefaultTlsConfig(tlsConfig)
				.build();

		var requestConfig = RequestConfig.custom()
				.setConnectionKeepAlive(TimeValue.ofMinutes(1))
				.build();

		var ioReactorConfig = IOReactorConfig.custom()
				.setIoThreadCount(Math.min(Runtime.getRuntime().availableProcessors(), 8))
				.setSoTimeout(HttpClientBuilder.TIMEOUT)
				.build();

		return HttpAsyncClientBuilder.create()
				.disableAuthCaching()
				.disableAutomaticRetries()
				.disableConnectionState()
				.disableCookieManagement()
				.setConnectionManager(connectionManager)
				.setDefaultHeaders(defaultHeaders)
				.setDefaultRequestConfig(requestConfig)
				.setIOReactorConfig(ioReactorConfig)
				.setRedirectStrategy(CustomRedirectStrategy.INSTANCE)
				.setThreadFactory(new DefaultThreadFactory("http", true))
				.build();
	}

}
