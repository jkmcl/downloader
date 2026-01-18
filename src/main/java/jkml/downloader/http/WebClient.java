package jkml.downloader.http;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

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

		// Set User-Agent header
		var userAgent = options.getUserAgent();
		request.setHeader(Headers.userAgent((userAgent == null) ? DEFAULT_USER_AGENT : userAgent));

		// Set Accept and Accept-Language headers
		request.setHeader(Headers.ACCEPT);
		request.setHeader(Headers.ACCEPT_LANGUAGE);

		// Set Referer header
		if (options.getReferer() == Referer.SELF) {
			// Get the URI from the request as BasicHttpRequest re-assembles it
			request.setHeader(HttpHeaders.REFERER, HttpUtils.getUri(request).toString());
		}

		// Set If-Modified-Since header
		var ifModifiedSince = options.getIfModifiedSince();
		if (ifModifiedSince != null) {
			HttpUtils.setTimeHeader(request, HttpHeaders.IF_MODIFIED_SINCE, ifModifiedSince);
		}

		return request;
	}

	private <T> T execute(HttpRequest request, HttpContext context, ResponseHandler<T> responseHandler) throws WebClientException {
		logger.info("Sending request to {}", HttpUtils.getUri(request));
		try {
			return httpClient.execute(new BasicRequestProducer(request, null), responseHandler, context, null).get();
		} catch (ExecutionException | InterruptedException e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			logger.error("Exception caught", e);
			var rootCause = LangUtils.getRootCause(e);
			throw new WebClientException("%s: %s".formatted(rootCause.getClass().getName(), rootCause.getMessage()));
		}
	}

	/**
	 * Retrieve the response body as a String.
	 */
	public TextResult getContent(URI uri, RequestOptions options) throws WebClientException {
		return execute(createRequest(uri, options), null, new TextResponseHandler());
	}

	/**
	 * Retrieve the response body and save it to file.
	 */
	public FileResult saveToFile(URI uri, RequestOptions options, Path path) throws IOException, WebClientException {
		if (Files.notExists(path)) {
			logger.debug("Local file does not exist: {}", path);
			var dir = path.getParent();
			if (dir != null) {
				Files.createDirectories(dir);
			}
		} else {
			logger.debug("Local file exists: {}", path);
			var lastMod = Files.getLastModifiedTime(path).toInstant();
			logger.atDebug().log("Local file last modified time: {}", TimeUtils.formatter.format(lastMod));
			options.setIfModifiedSince(lastMod);
		}
		return execute(createRequest(uri, options), null, new FileResponseHandler(uri, path));
	}

	/**
	 * Retrieve the location header value in the redirect (3xx) response.
	 */
	public LinkResult getLocation(URI uri, RequestOptions options) throws WebClientException {
		var context = new HttpClientContext();

		// Disable auto-redirect to obtain the location header
		context.setAttribute(CustomRedirectStrategy.DISABLE_REDIRECT, Boolean.TRUE);

		return execute(createRequest(uri, options), context, new LinkResponseHandler());
	}

}
