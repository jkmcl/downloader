package jkml.downloader.http;

import java.io.IOException;

import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;

/**
 * Clone of {@link DefaultHttpRequestRetryStrategy} with these differences:
 * <ul>
 * <li>No retry after I/O exception occurred
 * <li>Maximum retry count of 2 (instead of 1)
 * </ul>
 */
class CustomRetryStrategy extends DefaultHttpRequestRetryStrategy {

	public static final CustomRetryStrategy INSTANCE = new CustomRetryStrategy();

	private CustomRetryStrategy() {
		super(2, TimeValue.ofSeconds(1));
	}

	@Override
	public boolean retryRequest(HttpRequest request, IOException exception, int execCount, HttpContext context) {
		return false;
	}

}
