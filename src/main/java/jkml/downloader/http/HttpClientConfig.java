package jkml.downloader.http;

import java.util.List;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

class HttpClientConfig {

	public static final Timeout CONNECT_TIMEOUT = Timeout.ofSeconds(30);

	public static final Timeout SOCKET_TIMEOUT = Timeout.ofSeconds(30);

	private List<BasicHeader> defaultHeaders;

	private String userAgent;

	public List<BasicHeader> getDefaultHeaders() {
		return defaultHeaders;
	}

	public void setDefaultHeaders(List<BasicHeader> defaultHeaders) {
		this.defaultHeaders = defaultHeaders;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public ConnectionConfig createConnectionConfig() {
		return ConnectionConfig.custom()
				.setConnectTimeout(CONNECT_TIMEOUT)
				.setSocketTimeout(SOCKET_TIMEOUT)
				.setTimeToLive(TimeValue.ofMinutes(1))
				.setValidateAfterInactivity(TimeValue.ZERO_MILLISECONDS)
				.build();
	}

	public RequestConfig createRequestConfig() {
		return RequestConfig.custom()
				.setConnectionKeepAlive(TimeValue.ofMinutes(1))
				.build();
	}

}
