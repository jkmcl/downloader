package jkml.downloader.http;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.nio.support.BasicRequestProducer;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jkml.downloader.util.LangUtils;
import jkml.downloader.util.PropertiesHelper;
import jkml.downloader.util.TimeUtils;

public class WebClient implements Closeable {

	public static final UserAgent DEFAULT_USER_AGENT = UserAgent.CHROME;

	private final Logger logger = LoggerFactory.getLogger(WebClient.class);

	private final Map<UserAgent, String> userAgentStrings = new EnumMap<>(UserAgent.class);

	private final String acceptLanguage;

	private final CloseableHttpAsyncClient httpClient;

	public WebClient() {
		var propertiesHelper = new PropertiesHelper("http.properties");
		userAgentStrings.put(UserAgent.CHROME, propertiesHelper.getRequired("user-agent.chrome"));
		userAgentStrings.put(UserAgent.CURL, propertiesHelper.getRequired("user-agent.curl"));
		acceptLanguage = propertiesHelper.getRequired("accept-language");
		httpClient = new HttpClientBuilder().build();
		httpClient.start();
	}

	@Override
	public void close() throws IOException {
		httpClient.close();
	}

	private Instant getLastModifiedTime(Path path) {
		if (Files.notExists(path)) {
			logger.debug("Local file does not exist");
			return null;
		}

		logger.debug("Local file path: {}", path);

		try {
			var lastMod = Files.getLastModifiedTime(path).toInstant();
			logger.atDebug().log("Local file last modified time: {}", TimeUtils.Formatter.format(lastMod));
			return lastMod;
		} catch (IOException e) {
			throw new UncheckedIOException(e.getMessage(), e);
		}
	}

	String getUserAgentString(UserAgent userAgent) {
		return userAgentStrings.get(userAgent);
	}

	HttpRequest createRequest(URI uri, RequestOptions options) {
		logger.info("Creating request to {}", uri);

		var request = new BasicHttpRequest(Method.GET, uri);

		// Determine header values
		UserAgent userAgent = DEFAULT_USER_AGENT;
		Referer referer = null;
		if (options != null) {
			userAgent = (options.getUserAgent() != null) ? options.getUserAgent() : userAgent;
			referer = options.getReferer();
		}

		// Set User-Agent header
		var value = getUserAgentString(userAgent);
		if (userAgent != DEFAULT_USER_AGENT) {
			logger.debug("Setting custom {}: {}", HttpHeaders.USER_AGENT, value);
		}
		request.setHeader(HttpHeaders.USER_AGENT, value);

		// Set Accept-Language header
		request.setHeader(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage);

		// Set Referer header
		if (referer == Referer.SELF) {
			// Get the URI from the request as BasicHttpRequest re-assembles it
			value = HttpUtils.getUri(request).toString();
			logger.debug("Setting {}: {}", HttpHeaders.REFERER, value);
			request.setHeader(HttpHeaders.REFERER, value);
		}

		return request;
	}

	private <T> T execute(HttpRequest request, HttpContext conetxt, ResponseHandler<T> responseHandler, Function<Throwable, T> exceptionHandler) {
		logger.info("Sending request");
		try {
			return httpClient.execute(new BasicRequestProducer(request, null), responseHandler, conetxt, null).get();
		} catch (Exception e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			logger.error("Exception caught", e);
			return exceptionHandler.apply(LangUtils.getRootCause(e));
		}
	}

	/**
	 * Retrieve the response body as a String.
	 */
	public TextResult getContent(URI uri, RequestOptions options) {
		return execute(createRequest(uri, options), null, new ResponseToTextHandler(), TextResult::new);
	}

	/**
	 * Download the file if it does not exist locally or if it is newer than the
	 * local copy.
	 */
	public FileResult saveToFile(URI uri, RequestOptions options, Path path) {
		var request = createRequest(uri, options);

		// Add If-Modified-Since request header if local file exists
		var lastMod = getLastModifiedTime(path);
		if (lastMod != null) {
			HttpUtils.setTimeHeader(request, HttpHeaders.IF_MODIFIED_SINCE, lastMod);
		}

		return execute(request, null, new ResponseToFileHandler(path), FileResult::new);
	}

	/**
	 * Retrieve the location header value in the redirect (3xx) response.
	 */
	public LinkResult getLocation(URI uri, RequestOptions options) {
		var context = new BasicHttpContext();

		// Disable auto-redirect to obtain the location header
		context.setAttribute(CustomRedirectStrategy.DISABLE_REDIRECT, Boolean.TRUE);

		return execute(createRequest(uri, options), context, new ResponseToLinkHandler(), LinkResult::new);
	}

}
