package jkml.downloader.http;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.concurrent.DefaultThreadFactory;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.TimeValue;

class HttpClientBuilder {

	CloseableHttpAsyncClient build() {
		var connectionConfig = ConnectionConfig.custom()
				.setConnectTimeout(Constants.TIMEOUT)
				.setSocketTimeout(Constants.TIMEOUT)
				.setTimeToLive(Constants.TIME_TO_LIVE)
				.setValidateAfterInactivity(TimeValue.ZERO_MILLISECONDS)
				.build();

		var tlsConfig = TlsConfig.custom()
				.setHandshakeTimeout(Constants.TIMEOUT)
				.build();

		var connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
				.setDefaultConnectionConfig(connectionConfig)
				.setDefaultTlsConfig(tlsConfig)
				.build();

		var ioReactorConfig = IOReactorConfig.custom()
				.setIoThreadCount(Math.min(Runtime.getRuntime().availableProcessors(), 8))
				.setSoTimeout(Constants.TIMEOUT)
				.build();

		return HttpAsyncClientBuilder.create()
				.disableAuthCaching()
				.disableAutomaticRetries()
				.disableConnectionState()
				.disableCookieManagement()
				.setConnectionManager(connectionManager)
				.setDefaultRequestConfig(Constants.DEFAULT_REQUEST_CONFIG)
				.setIOReactorConfig(ioReactorConfig)
				.setRedirectStrategy(new CustomRedirectStrategy())
				.setThreadFactory(new DefaultThreadFactory("http", true))
				.build();
	}

}
