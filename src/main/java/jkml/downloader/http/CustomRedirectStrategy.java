package jkml.downloader.http;

import java.net.URI;

import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.protocol.RedirectStrategy;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.http.protocol.HttpContext;

/**
 * An implementation of {@link RedirectStrategy} that delegates to
 * {@link DefaultRedirectStrategy}, with additional support for the non-standard
 * Refresh header. Example:
 *
 * <pre>Refresh: 0; URL=https://www.foobar2000.org/files/foobar2000_v1.6.11.exe</pre>
 *
 * @see <a href="https://daniel.haxx.se/blog/2019/03/12/looking-for-the-refresh-header/">Looking for the Refresh header</a>
 */
class CustomRedirectStrategy implements RedirectStrategy {

	public static final String DISABLE_REDIRECT = "downloader.disable-redirect";

	@Override
	public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
		if (Boolean.TRUE.equals(context.getAttribute(DISABLE_REDIRECT))) {
			return false;
		}

		if (DefaultRedirectStrategy.INSTANCE.isRedirected(request, response, context)) {
			return true;
		}

		if (response.getCode() != HttpStatus.SC_OK) {
			return false;
		}

		var location = HttpUtils.getParameter(response, "Refresh", "URL");
		if (location == null) {
			return false;
		}

		// Put destination in this header for super class to extract
		response.setHeader(HttpHeaders.LOCATION, location);
		return true;
	}

	@Override
	public URI getLocationURI(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException {
		return DefaultRedirectStrategy.INSTANCE.getLocationURI(request, response, context);
	}

}
