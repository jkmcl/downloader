package jkml.downloader.http;

import java.util.ArrayList;
import java.util.Collections;
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

	public static final Timeout TIMEOUT = Timeout.ofSeconds(30);

	private String userAgent;

	private final List<Header> defaultHeaders = new ArrayList<>();

	HttpClientBuilder userAgent(String value) {
		userAgent = value;
		return this;
	}

	HttpClientBuilder defaultHeaders(Header... values) {
		Collections.addAll(defaultHeaders, values);
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

		var requestConfig = RequestConfig.custom()
				.setConnectionKeepAlive(TimeValue.ofMinutes(1))
				.build();


		var ioReactorConfig = IOReactorConfig.custom()
				.setSoTimeout(HttpClientBuilder.TIMEOUT)
				.setIoThreadCount(Math.min(Runtime.getRuntime().availableProcessors(), 8))
				.build();

		var connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
				.setDefaultConnectionConfig(connectionConfig)
				.setDefaultTlsConfig(tlsConfig)
				.build();

		return HttpAsyncClientBuilder.create()
				.disableAuthCaching()
				.disableAutomaticRetries()
				.disableConnectionState()
				.disableCookieManagement()
				.setConnectionManager(connectionManager)
				.setDefaultHeaders(defaultHeaders)
				.setDefaultRequestConfig(requestConfig)
				.setRedirectStrategy(CustomRedirectStrategy.INSTANCE)
				.setUserAgent(userAgent)
				.setIOReactorConfig(ioReactorConfig)
				.setThreadFactory(new DefaultThreadFactory("http", true))
				.build();
	}

}
