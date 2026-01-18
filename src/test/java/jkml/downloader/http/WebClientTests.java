package jkml.downloader.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.seeOther;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import jkml.downloader.util.FileUtils;
import jkml.downloader.util.StringUtils;
import jkml.downloader.util.TestUtils;

class WebClientTests {

	private static final String MOCK_URL_PATH = "/dir1/file1";

	private static final Logger logger = LoggerFactory.getLogger(WebClientTests.class);

	private static final Path outDir = TestUtils.outputDirectory();

	private static URI mockUrl;

	private WebClient webClient;

	@RegisterExtension
	private static final WireMockExtension wireMockExt = WireMockExtension.newInstance().options(WireMockConfiguration.wireMockConfig().dynamicPort()).build();

	@BeforeAll
	static void beforeAll() {
		mockUrl = URI.create(wireMockExt.getRuntimeInfo().getHttpBaseUrl() + MOCK_URL_PATH);
	}

	@BeforeEach
	void beforeEach(TestInfo testInfo) {
		logger.info("# Executing {}", testInfo.getDisplayName());
		webClient = new WebClient();
	}

	@AfterEach
	void afterEach() {
		webClient.close();
		logger.info(StringUtils.EMPTY);
	}

	@Test
	void testCreateRequest() {
		var defaultUserAgent = Headers.userAgent(WebClient.DEFAULT_USER_AGENT);

		var options = new RequestOptions();
		var request = webClient.createRequest(mockUrl, options);
		assertEquals(defaultUserAgent, request.getFirstHeader(HttpHeaders.USER_AGENT));
		assertNull(request.getFirstHeader(HttpHeaders.REFERER));
		assertNull(request.getFirstHeader(HttpHeaders.IF_MODIFIED_SINCE));

		options.setUserAgent(UserAgent.CHROME);
		request = webClient.createRequest(mockUrl, options);
		assertEquals(Headers.userAgent(UserAgent.CHROME), request.getFirstHeader(HttpHeaders.USER_AGENT));

		options.setUserAgent(UserAgent.CURL);
		request = webClient.createRequest(mockUrl, options);
		assertEquals(Headers.userAgent(UserAgent.CURL), request.getFirstHeader(HttpHeaders.USER_AGENT));

		options.setReferer(Referer.SELF);
		request = webClient.createRequest(mockUrl, options);
		assertEquals(mockUrl.toString(), request.getFirstHeader(HttpHeaders.REFERER).getValue());

		options.setIfModifiedSince(Instant.now());
		request = webClient.createRequest(mockUrl, options);
		assertEquals(DateUtils.formatStandardDate(options.getIfModifiedSince()),
				request.getFirstHeader(HttpHeaders.IF_MODIFIED_SINCE).getValue());
	}

	@Test
	void testGetContent_Success() throws Exception {
		wireMockExt.stubFor(get(urlPathEqualTo(MOCK_URL_PATH)).willReturn(ok("Hello world!")));

		var result = webClient.getContent(mockUrl, new RequestOptions());
		assertEquals(Status.OK, result.status());
		assertFalse(StringUtils.isNullOrBlank(result.text()));
	}

	@Test
	void testGetContent_Failure() {
		wireMockExt.stubFor(get(urlPathEqualTo(MOCK_URL_PATH)).willReturn(notFound()));

		assertThrows(WebClientException.class, () -> webClient.getContent(mockUrl, new RequestOptions()));
	}

	private void testSaveToFile_OK(boolean directoryExists) throws Exception {
		wireMockExt.stubFor(get(urlPathEqualTo(MOCK_URL_PATH)).willReturn(
				ok("Hello world!").withHeader(HttpHeaders.LAST_MODIFIED, DateUtils.formatStandardDate(Instant.now()))));

		var localFilePath = outDir.resolve(FileUtils.getFileName(mockUrl));
		Files.deleteIfExists(localFilePath);
		if (directoryExists) {
			Files.createDirectories(outDir);
		} else {
			TestUtils.deleteDirectories(outDir);
		}

		var result = webClient.saveToFile(mockUrl, new RequestOptions(), localFilePath);

		assertEquals(Status.OK, result.status());
		assertTrue(Files.exists(localFilePath));
	}

	@Test
	void testSaveToFile_OK_dirNotExists() throws Exception {
		testSaveToFile_OK(false);
	}

	@Test
	void testSaveToFile_OK_dirExists() throws Exception {
		testSaveToFile_OK(true);
	}

	@Test
	void testSaveToFile_NotModified() throws Exception {
		wireMockExt.stubFor(get(urlPathEqualTo(MOCK_URL_PATH)).willReturn(aResponse().withStatus(304)));

		var localFilePath = outDir.resolve(FileUtils.getFileName(mockUrl));
		Files.createDirectories(outDir);
		Files.writeString(localFilePath, StringUtils.EMPTY);
		Files.setLastModifiedTime(localFilePath, FileTime.from(Instant.now()));

		var result = webClient.saveToFile(mockUrl, new RequestOptions(), localFilePath);

		assertEquals(Status.NOT_MODIFIED, result.status());
		assertTrue(Files.exists(localFilePath));
	}

	@Test
	void testSaveToFile_Failure() throws IOException {
		wireMockExt.stubFor(get(urlPathEqualTo(MOCK_URL_PATH)).willReturn(notFound()));

		var localFilePath = outDir.resolve(FileUtils.getFileName(mockUrl));
		Files.deleteIfExists(localFilePath);
		assertThrows(WebClientException.class, () -> webClient.saveToFile(mockUrl, new RequestOptions(), localFilePath));
	}

	@Test
	void testGetLocation_Success() throws Exception {
		var location = "http://localhost/file.txt";
		wireMockExt.stubFor(get(urlPathEqualTo(MOCK_URL_PATH)).willReturn(seeOther(location)));

		var result = webClient.getLocation(mockUrl, new RequestOptions());

		assertEquals(Status.OK, result.status());
		assertEquals(URI.create(location), result.link());
	}

	@Test
	void testGetLocation_Failure() {
		wireMockExt.stubFor(get(urlPathEqualTo(MOCK_URL_PATH)).willReturn(ok()));

		assertThrows(WebClientException.class, () -> webClient.getLocation(mockUrl, new RequestOptions()));
	}

}
