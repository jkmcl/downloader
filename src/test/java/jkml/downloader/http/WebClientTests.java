package jkml.downloader.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
	void afterEach() throws IOException {
		webClient.close();
		logger.info(StringUtils.EMPTY);
	}

	@Test
	void testReadString_Success() throws Exception {
		wireMockExt.stubFor(get(urlPathEqualTo(MOCK_URL_PATH)).willReturn(ok("Hello world!")));

		var result = webClient.readString(mockUrl);
		assertFalse(StringUtils.isNullOrBlank(result.text()));
	}

	@Test
	void testReadString_Failure() throws Exception {
		wireMockExt.stubFor(get(urlPathEqualTo(MOCK_URL_PATH)).willReturn(notFound()));

		var result = webClient.readString(mockUrl);
		assertNull(null, result.text());
	}

	@Test
	void testReadString_CustomOptions() throws Exception {
		wireMockExt.stubFor(get(urlPathEqualTo(MOCK_URL_PATH)).willReturn(ok("Hello world!")));

		var options = new RequestOptions(UserAgent.CURL, Referer.SELF);

		var result = webClient.readString(mockUrl, options);
		assertFalse(StringUtils.isNullOrBlank(result.text()));
	}

	@Test
	void testSaveToFile_OK() throws Exception {
		wireMockExt.stubFor(get(urlPathEqualTo(MOCK_URL_PATH)).willReturn(
				ok("Hello world!").withHeader(HttpHeaders.LAST_MODIFIED, DateUtils.formatStandardDate(Instant.now()))));

		Files.createDirectories(outDir);
		var localFilePath = outDir.resolve(FileUtils.getFileName(mockUrl));

		var result = webClient.saveToFile(mockUrl, null, localFilePath);

		assertEquals(Status.OK, result.status());
		assertEquals(localFilePath, result.filePath());
		assertTrue(Files.exists(localFilePath));

		Files.delete(localFilePath);
	}

	@Test
	void testSaveToFile_NotModified() throws Exception {
		wireMockExt.stubFor(get(urlPathEqualTo(MOCK_URL_PATH)).willReturn(aResponse().withStatus(304)));

		Files.createDirectories(outDir);
		var localFilePath = outDir.resolve(FileUtils.getFileName(mockUrl));
		Files.writeString(localFilePath, StringUtils.EMPTY);
		Files.setLastModifiedTime(localFilePath, FileTime.from(Instant.now()));

		var result = webClient.saveToFile(mockUrl, null, outDir.resolve(FileUtils.getFileName(mockUrl)));

		assertEquals(Status.NOT_MODIFIED, result.status());
		assertEquals(localFilePath, result.filePath());
		assertTrue(Files.exists(localFilePath));

		Files.delete(localFilePath);
	}

}
