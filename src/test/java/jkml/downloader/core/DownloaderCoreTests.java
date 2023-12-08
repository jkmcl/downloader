package jkml.downloader.core;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jkml.downloader.http.RequestOptions;
import jkml.downloader.http.SaveResult;
import jkml.downloader.http.WebClient;
import jkml.downloader.profile.Profile;
import jkml.downloader.profile.Profile.Type;
import jkml.downloader.util.TestUtils;

class DownloaderCoreTests {

	private static final String PROFILES_JSON_FILE_NAME = "profiles.json";

	private final Logger logger = LoggerFactory.getLogger(DownloaderCoreTests.class);

	private static Profile createProfile(Type type) {
		var profile = new Profile();
		profile.setName("name");
		profile.setType(type);
		profile.setOutputDirectory(TestUtils.getOutputDirectory());
		return profile;
	}

	@BeforeEach
	void beforeEach(TestInfo testInfo) {
		logger.atInfo().log("# Executing {}", testInfo.getDisplayName());
	}

	@AfterEach
	void afterEach() {
		logger.atInfo().log();
	}

	@Test
	void testDownload() throws IOException {
		try (var mockedWebClient = mock(WebClient.class); var core = new DownloaderCore(mockedWebClient)) {
			core.download(TestUtils.getResoureAsPath(PROFILES_JSON_FILE_NAME));

			verify(mockedWebClient, times(4)).readString(isA(URI.class), isA(RequestOptions.class));
			verify(mockedWebClient, times(1)).saveToFile(isA(URI.class), isA(RequestOptions.class), isA(Path.class));
		}
	}

	@Test
	void testDownload_direct() throws IOException {
		var profile = createProfile(Type.STANDARD);
		profile.setFileUrl(URI.create("https://localhost/file.zip"));

		var result = new SaveResult(SaveResult.Status.NOT_MODIFIED, Path.of("/tmp/file.zip"));

		try (var mockedWebClient = mock(WebClient.class); var core = new DownloaderCore(mockedWebClient)) {
			when(mockedWebClient.saveToFile(any(), any(), any())).thenReturn(result);

			core.download(profile);

			verify(mockedWebClient, times(1)).saveToFile(isA(URI.class), isA(RequestOptions.class), isA(Path.class));
		}
	}

	@Test
	void testDownload_versionInFileLink() throws IOException {
		var profile = createProfile(Type.STANDARD);
		profile.setPageUrl(URI.create("https://localhost/page.html"));
		profile.setLinkPattern(Pattern.compile(" href=\"(file\\.zip)\""));

		var result = new SaveResult(SaveResult.Status.NOT_MODIFIED, Path.of("/tmp/file.zip"));

		try (var mockedWebClient = mock(WebClient.class); var core = new DownloaderCore(mockedWebClient)) {
			when(mockedWebClient.readString(any(), any())).thenReturn("<a href=\"file.zip\">Version 1.0</a>");
			when(mockedWebClient.saveToFile(any(), any(), any())).thenReturn(result);

			core.download(profile);

			verify(mockedWebClient, times(1)).readString(isA(URI.class), isA(RequestOptions.class));
			verify(mockedWebClient, times(1)).saveToFile(isA(URI.class), isA(RequestOptions.class), isA(Path.class));
		}
	}

	@Test
	void testDownload_versionInPage() throws IOException {
		var profile = createProfile(Type.STANDARD);
		profile.setPageUrl(URI.create("https://localhost/page.html"));
		profile.setLinkPattern(Pattern.compile(" href=\"(file\\.zip)\""));
		profile.setVersionPattern(Pattern.compile("Version (\\d\\.\\d)"));

		var result = new SaveResult(SaveResult.Status.NOT_MODIFIED, Path.of("/tmp/file.zip"));

		try (var mockedWebClient = mock(WebClient.class); var core = new DownloaderCore(mockedWebClient)) {
			when(mockedWebClient.readString(any(), any())).thenReturn("<a href=\"file.zip\">Version 1.0</a>");
			when(mockedWebClient.saveToFile(any(), any(), any())).thenReturn(result);

			core.download(profile);

			verify(mockedWebClient, times(1)).readString(isA(URI.class), isA(RequestOptions.class));
			verify(mockedWebClient, times(1)).saveToFile(isA(URI.class), isA(RequestOptions.class), isA(Path.class));
		}
	}

	@Test
	void testDownload_Mozilla() throws IOException {
		var profile = createProfile(Type.MOZILLA);
		profile.setPageUrl(URI.create("https://localhost/pub/firefox/releases/"));
		profile.setLinkPattern(Pattern.compile("win64/en-US/Firefox"));

		var result = new SaveResult(SaveResult.Status.NOT_MODIFIED, Path.of("/tmp/file.zip"));

		try (var mockedWebClient = mock(WebClient.class); var core = new DownloaderCore(mockedWebClient)) {
			when(mockedWebClient.readString(any(), any())).thenReturn(TestUtils.readResourceAsString("firefox.html"));
			when(mockedWebClient.saveToFile(any(), any(), any())).thenReturn(result);

			core.download(profile);

			verify(mockedWebClient, times(1)).readString(isA(URI.class), isA(RequestOptions.class));
			verify(mockedWebClient, times(1)).saveToFile(isA(URI.class), isA(RequestOptions.class), isA(Path.class));
		}
	}

	@Test
	void testDownload_GitHub() throws IOException {
		var profile = createProfile(Type.GITHUB);
		profile.setPageUrl(URI.create("https://localhost/page.html"));
		profile.setLinkPattern(Pattern.compile(" href=\"(file\\.zip)\""));
		profile.setVersionPattern(Pattern.compile("Version (\\d\\.\\d)"));

		var html1 = "<include-fragment loading=\"lazy\" src=\"https://localhost/account/project/releases/expanded_assets/v1.0\" >"
				+ "<include-fragment loading=\"lazy\" src=\"https://localhost/account/project/releases/expanded_assets/v0.1\" >";

		var html2 = "<a href=\"file.zip\">Version 1.0</a>";

		var result = new SaveResult(SaveResult.Status.NOT_MODIFIED, Path.of("/tmp/file.zip"));

		try (var mockedWebClient = mock(WebClient.class); var core = new DownloaderCore(mockedWebClient)) {
			when(mockedWebClient.readString(eq(URI.create("https://localhost/page.html")), any())).thenReturn(html1);
			when(mockedWebClient.readString(eq(URI.create("https://localhost/account/project/releases/expanded_assets/v1.0")), any())).thenReturn(html2);
			when(mockedWebClient.saveToFile(any(), any(), any())).thenReturn(result);

			core.download(profile);

			verify(mockedWebClient, times(2)).readString(isA(URI.class), isA(RequestOptions.class));
			verify(mockedWebClient, times(1)).saveToFile(isA(URI.class), isA(RequestOptions.class), isA(Path.class));
		}
	}

	@Test
	void testPrintResult() {
		var fileUri = URI.create("https://localhost/index.html");
		try (var mockedWebClient = mock(WebClient.class); var core = new DownloaderCore(mockedWebClient)) {
			var result = new SaveResult(SaveResult.Status.OK, Path.of("index.html"), Instant.now());
			core.printResult(fileUri, result);

			result = new SaveResult(SaveResult.Status.NOT_MODIFIED, Path.of("index.html"));
			core.printResult(fileUri, result);

			result = new SaveResult(SaveResult.Status.ERROR, "Some error");
			core.printResult(fileUri, result);
		} catch (Exception e) {
			fail();
		}
	}

}
