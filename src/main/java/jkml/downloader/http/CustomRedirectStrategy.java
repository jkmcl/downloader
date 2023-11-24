package jkml.downloader.http;

import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.http.protocol.HttpContext;

/**
 * Handles redirect behavior supported by browsers according to the non-standard
 * Refresh header. Example:
 *
 * <pre>Refresh: 0; URL=https://www.foobar2000.org/files/foobar2000_v1.6.11.exe</pre>
 *
 * @see <a href="https://daniel.haxx.se/blog/2019/03/12/looking-for-the-refresh-header/">Looking for the Refresh header</a>
 */
class CustomRedirectStrategy extends DefaultRedirectStrategy {

	public static final CustomRedirectStrategy INSTANCE = new CustomRedirectStrategy();

	@Override
	public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
		// Let default strategy handle standard redirects
		if (super.isRedirected(request, response, context)) {
			return true;
		}

		if (response.getCode() != HttpStatus.SC_OK) {
			return false;
		}

		var location = HttpUtils.getFirstParameter(response, "Refresh", "URL");
		if (location == null) {
			return false;
		}

		response.setHeader(HttpHeaders.LOCATION, location);
		return true;
	}

}
