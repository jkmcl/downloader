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

	static void setTimeHeader(HttpMessage message, String name, Instant value) {
		message.setHeader(name, DateUtils.formatStandardDate(value));
	}

	static Instant getTimeHeader(HttpMessage message, String name) {
		var value = getHeader(message, name);
		return (value == null) ? null : DateUtils.parseStandardDate(value);
	}

	static String getHeader(HttpMessage message, String name) {
		var header = message.getFirstHeader(name);
		return (header == null) ? null : header.getValue();
	}

	static String getParameter(HttpMessage message, String headerName, String parameterName) {
		var header = message.getFirstHeader(headerName);
		if (header == null) {
			return null;
		}

		for (var element : MessageSupport.parseElements(header)) {
			var parameter = element.getParameterByName(parameterName);
			if (parameter != null) {
				return parameter.getValue();
			}
		}

		return null;
	}

	static URI getUri(HttpRequest request) {
		try {
			return request.getUri();
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

}
