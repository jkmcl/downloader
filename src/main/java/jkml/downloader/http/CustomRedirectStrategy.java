package jkml.downloader.http;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.utils.URIUtils;
import org.apache.hc.core5.http.HttpException;
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

	private static final String REFRESH_URL = "custom.http.refresh.header.url";

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

		// Add Refresh header URL to context
		context.setAttribute(REFRESH_URL, location);
		return true;
	}

    @Override
    public URI getLocationURI(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException {
		// Remove Refresh header URL from context
		var location = (String) context.removeAttribute(REFRESH_URL);

		// Let default strategy handle standard redirects
		if (location == null) {
			return super.getLocationURI(request, response, context);
		}

		// Resolve URL exactly like how default strategy does it
		var uri = createLocationURI(location);
		try {
			if (!uri.isAbsolute()) {
				// Resolve location URI
				uri = URIUtils.resolve(request.getUri(), uri);
			}
		} catch (URISyntaxException ex) {
			throw new ProtocolException(ex.getMessage(), ex);
		}

		return uri;
	}

}
