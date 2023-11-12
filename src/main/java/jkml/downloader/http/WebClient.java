package jkml.downloader.http;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jkml.downloader.http.SaveResult.Status;
import jkml.downloader.util.LangUtils;
import jkml.downloader.util.PropertiesHelper;
import jkml.downloader.util.TimeUtils;

public class WebClient implements Closeable {

	private static final UserAgent DEFAULT_USER_AGENT = UserAgent.CHROME;

	private final Logger logger = LoggerFactory.getLogger(WebClient.class);

	private final Map<UserAgent, String> userAgentStrings = new EnumMap<>(UserAgent.class);

	private final HttpClientAdapter httpClient;

	public WebClient(boolean classic) {
		var propertiesHelper = new PropertiesHelper("http.properties");
		var chromeUserAgent = propertiesHelper.getRequiredProperty("user-agent.chrome");
		var curlUserAgent = propertiesHelper.getRequiredProperty("user-agent.curl");
		var acceptLanguage = propertiesHelper.getRequiredProperty("accept-language");

		userAgentStrings.put(UserAgent.CHROME, chromeUserAgent);
		userAgentStrings.put(UserAgent.CURL, curlUserAgent);

		var config = new HttpClientConfig();
		config.setUserAgent(userAgentStrings.get(DEFAULT_USER_AGENT));
		config.setDefaultHeaders(List.of(new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage)));

		httpClient = classic ? new HttpClassicClientAdapter() : new HttpAsyncClientAdapter();
		logger.atDebug().log("HTTP client adapter class: {}", httpClient.getClass().getSimpleName());
		httpClient.initialize(config);
	}

	@Override
	public void close() throws IOException {
		httpClient.close();
	}

	private Instant getLastModifiedTime(Path filePath) {
		if (Files.notExists(filePath)) {
			logger.atDebug().log("Local file does not exist");
			return null;
		}

		logger.atDebug().log("Local file path: {}", filePath);

		try {
			var lastMod = Files.getLastModifiedTime(filePath).toInstant();
			logger.atDebug().log("Local file last modified time: {}", TimeUtils.FORMATTER.format(lastMod));
			return lastMod;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private HttpRequest createRequest(Method method, URI uri, RequestOptions options) {
		logger.atInfo().log("Creating {} request to {}", method, uri);

		var request = httpClient.createRequest(method, uri);

		if (options == null) {
			return request;
		}

		if (options.userAgent() != null && options.userAgent() != DEFAULT_USER_AGENT) {
			var ua = userAgentStrings.get(options.userAgent());
			logger.atDebug().log("Setting custom {}: {}", HttpHeaders.USER_AGENT, ua);
			request.setHeader(HttpHeaders.USER_AGENT, ua);
		}

		if (options.referer() == Referer.SELF) {
			String referer;
			try {
				referer = request.getUri().toString();
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(e.getMessage(), e);
			}
			logger.atDebug().log("Setting {}: {}", HttpHeaders.REFERER, referer);
			request.setHeader(HttpHeaders.REFERER, referer);
		}

		return request;
	}

	private <T> T execute(HttpRequest request, ResponseHandler<T> responseHandler, Function<Exception, T> exceptionHandler) {
		logger.atInfo().log("Sending request");
		try {
			return httpClient.execute(request, responseHandler);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return exceptionHandler.apply(e);
		} catch (Exception e) {
			return exceptionHandler.apply(e);
		}
	}

	/**
	 * Download content and return it as a String. The content is converted using
	 * the encoding in the response header (if any), failing that, "ISO-8859-1" is
	 * used.
	 */
	public String readString(URI uri) {
		return readString(uri, null);
	}

	public String readString(URI uri, RequestOptions options) {
		return execute(createRequest(Method.GET, uri, options), new ResponseToTextHandler(), e -> {
			logger.atError().setCause(e).log("Exception caught");
			return null;
		});
	}

	/**
	 * Download remote file if local file does not exist or is older than remote
	 * file
	 */
	public SaveResult saveToFile(URI uri, RequestOptions options, Path filePath) {
		var lastMod = getLastModifiedTime(filePath);

		var request = createRequest(Method.GET, uri, options);

		// Add If-Modified-Since request header if local file exists
		if (lastMod != null) {
			HttpUtils.setTimeHeader(request, HttpHeaders.IF_MODIFIED_SINCE, lastMod);
		}

		return execute(request, new ResponseToFileHandler(uri, filePath), e -> {
			logger.atError().setCause(e).log("Exception caught");
			var rootCause = LangUtils.getRootCause(e);
			return new SaveResult(Status.ERROR, rootCause.getClass().getName() + ": " + rootCause.getMessage());
		});
	}

}
