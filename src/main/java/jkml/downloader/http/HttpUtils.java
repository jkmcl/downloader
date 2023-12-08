package jkml.downloader.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;

import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.http.HttpMessage;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.message.MessageSupport;

class HttpUtils {

	private HttpUtils() {
	}

	public static void setTimeHeader(HttpMessage message, String name, Instant value) {
		message.setHeader(name, DateUtils.formatStandardDate(value));
	}

	public static Instant getTimeHeader(HttpMessage message, String name) {
		var header = message.getFirstHeader(name);
		if (header == null) {
			return null;
		}

		var value = header.getValue();
		if (value == null) {
			return null;
		}

		return DateUtils.parseStandardDate(value);
	}

	public static String getFirstParameter(HttpMessage message, String headerName, String parameterName) {
		var header = message.getFirstHeader(headerName);
		if (header == null) {
			return null;
		}

		for (var headerElement : MessageSupport.parse(header)) {
			var nameValuePair = headerElement.getParameterByName(parameterName);
			if (nameValuePair != null) {
				return nameValuePair.getValue();
			}
		}

		return null;
	}

	public static URI getUri(HttpRequest request) {
		try {
			return request.getUri();
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

}
