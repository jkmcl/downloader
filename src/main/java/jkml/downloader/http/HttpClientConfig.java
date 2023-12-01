package jkml.downloader.http;

import java.util.ArrayList;
import java.util.List;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

class HttpClientConfig {

	public static final Timeout SOCKET_TIMEOUT = Timeout.ofSeconds(30);

	private final ConnectionConfig connectionConfig;

	private final RequestConfig requestConfig;

	private final String userAgent;

	private final List<Header> defaultHeaders = new ArrayList<>();

	public HttpClientConfig(String userAgent) {
		connectionConfig = ConnectionConfig.custom()
				.setSocketTimeout(SOCKET_TIMEOUT)
				.setConnectTimeout(SOCKET_TIMEOUT)
				.setTimeToLive(TimeValue.ofMinutes(1))
				.setValidateAfterInactivity(TimeValue.ZERO_MILLISECONDS)
				.build();

		requestConfig = RequestConfig.custom()
				.setConnectionKeepAlive(TimeValue.ofMinutes(1))
				.build();

		this.userAgent = userAgent;
	}

	public ConnectionConfig getConnectionConfig() {
		return connectionConfig;
	}

	public RequestConfig getRequestConfig() {
		return requestConfig;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public List<Header> getDefaultHeaders() {
		return defaultHeaders;
	}

}
