package jkml.downloader.http;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.function.Function;

import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.nio.support.BasicRequestProducer;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jkml.downloader.util.LangUtils;
import jkml.downloader.util.TimeUtils;

public class WebClient implements Closeable {

	public static final UserAgent DEFAULT_USER_AGENT = UserAgent.CHROME;

	private final Logger logger = LoggerFactory.getLogger(WebClient.class);

	private final CloseableHttpAsyncClient httpClient;

	public WebClient() {
		httpClient = new HttpClientBuilder().build();
		httpClient.start();
	}

	@Override
	public void close() {
		try {
			httpClient.close();
		} catch (IOException e) {
			logger.error("Failed to close HTTP client", e);
		}
	}

	HttpRequest createRequest(URI uri, RequestOptions options) {
		var request = new BasicHttpRequest(Method.GET, uri);

		UserAgent userAgent = null;
		Referer referer = null;
		if (options != null) {
			userAgent = options.getUserAgent();
			referer = options.getReferer();
		}

		// Set User-Agent header
		if (userAgent == null) {
			userAgent = DEFAULT_USER_AGENT;
		}
		request.setHeader(Headers.userAgent(userAgent));

		// Set Accept, Accept-Encoding and Accept-Language headers
		request.setHeader(Headers.ACCEPT);
		request.setHeader(Headers.ACCEPT_ENCODING);
		request.setHeader(Headers.ACCEPT_LANGUAGE);

		// Set Referer header
		if (referer == Referer.SELF) {
			// Get the URI from the request as BasicHttpRequest re-assembles it
			request.setHeader(HttpHeaders.REFERER, HttpUtils.getUri(request).toString());
		}

		return request;
	}

	private <T> T execute(HttpRequest request, HttpContext context, ResponseHandler<T> responseHandler, Function<Throwable, T> exceptionHandler) {
		logger.info("Sending request to {}", HttpUtils.getUri(request));
		try {
			return httpClient.execute(new BasicRequestProducer(request, null), responseHandler, context, null).get();
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
		return execute(createRequest(uri, options), null, new TextResponseHandler(), ResultUtils::textResult);
	}

	private static Path createDirectories(Path dir) {
		try {
			return Files.createDirectories(dir);
		} catch (IOException e) {
			throw new UncheckedIOException(e.getMessage(), e);
		}
	}

	private static Instant getLastModifiedTime(Path path) {
		try {
			return Files.getLastModifiedTime(path).toInstant();
		} catch (IOException e) {
			throw new UncheckedIOException(e.getMessage(), e);
		}
	}

	/**
	 * Download the file if it does not exist locally or if it is newer than the
	 * local copy.
	 */
	public FileResult saveToFile(URI uri, RequestOptions options, Path path) {
		var request = createRequest(uri, options);

		// Add If-Modified-Since request header if local file exists, otherwise create
		// its parent directory
		if (Files.notExists(path)) {
			logger.debug("Local file does not exist: {}", path);
			var dir = path.getParent();
			if (dir != null && Files.notExists(dir)) {
				createDirectories(dir);
			}
		} else {
			logger.debug("Local file exists: {}", path);
			var lastMod = getLastModifiedTime(path);
			logger.atDebug().log("Local file last modified time: {}", TimeUtils.Formatter.format(lastMod));
			HttpUtils.setTimeHeader(request, HttpHeaders.IF_MODIFIED_SINCE, lastMod);
		}

		return execute(request, null, new FileResponseHandler(uri, path), ResultUtils::fileResult);
	}

	/**
	 * Retrieve the location header value in the redirect (3xx) response.
	 */
	public LinkResult getLocation(URI uri, RequestOptions options) {
		var context = new HttpClientContext();

		// Disable auto-redirect to obtain the location header
		context.setAttribute(CustomRedirectStrategy.DISABLE_REDIRECT, Boolean.TRUE);

		return execute(createRequest(uri, options), context, new LinkResponseHandler(), ResultUtils::linkResult);
	}

}
