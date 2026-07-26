package jkml.downloader.http;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

class Constants {

	static final TimeValue TIME_TO_LIVE = TimeValue.ofMinutes(1);

	static final Timeout TIMEOUT = Timeout.ofSeconds(30);

	static final RequestConfig DEFAULT_REQUEST_CONFIG = RequestConfig.custom()
			.setConnectionKeepAlive(TIME_TO_LIVE)
			.setConnectionRequestTimeout(TIMEOUT)
			.build();

	static final RequestConfig NO_REDIRECT_REQUEST_CONFIG = RequestConfig.copy(DEFAULT_REQUEST_CONFIG)
			.setRedirectsEnabled(false)
			.build();

	private Constants() {
	}

}
