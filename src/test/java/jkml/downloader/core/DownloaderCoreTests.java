package jkml.downloader.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import jkml.downloader.http.FileResult;
import jkml.downloader.http.RequestOptions;
import jkml.downloader.http.Status;
import jkml.downloader.http.TextResult;
import jkml.downloader.http.WebClient;
import jkml.downloader.profile.Profile;
import jkml.downloader.profile.Profile.Type;
import jkml.downloader.util.StringUtils;
import jkml.downloader.util.TestUtils;

class DownloaderCoreTests {

	private static final String PROFILES_JSON_FILE_NAME = "profiles.json";

	private static final Path outDir = TestUtils.outputDirectory();

	private final Logger logger = LoggerFactory.getLogger(DownloaderCoreTests.class);

	private static Profile createProfile(Type type) {
		var profile = new Profile();
		profile.setName("Something");
		profile.setType(type);
		profile.setOutputDirectory(outDir);
		return profile;
	}

	@BeforeEach
	void beforeEach(TestInfo testInfo) {
		logger.info("# Executing {}", testInfo.getDisplayName());
	}

	@AfterEach
	void afterEach() {
		logger.info(StringUtils.EMPTY);
	}

	private static TextResult text(String text) {
		return new TextResult(Status.OK, text);
	}

	private static TextResult textNotFound() {
		return new TextResult(Status.ERROR, new Exception("Not found"));
	}

	private static FileResult fileNotModified() {
		return new FileResult(Status.NOT_MODIFIED, Path.of("/tmp/file.zip"));
	}

	private static RequestOptions anyRequestOptions() {
		return any(RequestOptions.class);
	}

	@Test
	void testDownload() throws IOException {
		try (var mockWebClient = mock(WebClient.class); var core = new DownloaderCore(mockWebClient)) {
			when(mockWebClient.readString(any(URI.class), anyRequestOptions())).thenReturn(text(""));
			when(mockWebClient.saveToFile(any(URI.class), anyRequestOptions(), any(Path.class))).thenReturn(fileNotModified());

			core.download(TestUtils.getResoureAsPath(PROFILES_JSON_FILE_NAME));

			verify(mockWebClient, times(4)).readString(any(URI.class), anyRequestOptions());
			verify(mockWebClient).saveToFile(any(URI.class), anyRequestOptions(), any(Path.class));
		}
	}

	private static void assertDownload(WebClient mock, URI fileUri, Path filePath) {
		verify(mock).saveToFile(eq(fileUri), anyRequestOptions(), eq(filePath));
	}

	@Test
	void testDownload_direct() throws IOException {
		var fileUri = URI.create("https://localhost/downloads/file-1.0.zip");
		var filePath = outDir.resolve("file-1.0.zip");

		var profile = createProfile(Type.STANDARD);
		profile.setFileUrl(fileUri);

		try (var mockWebClient = mock(WebClient.class); var core = new DownloaderCore(mockWebClient)) {
			when(mockWebClient.saveToFile(eq(fileUri), anyRequestOptions(), eq(filePath))).thenReturn(fileNotModified());

			core.download(profile);

			assertDownload(mockWebClient, fileUri, filePath);
		}
	}

	@Test
	void testDownload_noPage() throws IOException {
		var pageUri = URI.create("https://localhost/downloads/page.html");

		var profile = createProfile(Type.STANDARD);
		profile.setPageUrl(pageUri);
		profile.setLinkPattern(Pattern.compile(" href=\"(.+/file-[.0-9]+\\.zip)\""));

		try (var mockWebClient = mock(WebClient.class); var core = new DownloaderCore(mockWebClient)) {
			when(mockWebClient.readString(eq(pageUri), anyRequestOptions())).thenReturn(textNotFound());

			core.download(profile);

			verify(mockWebClient).readString(eq(pageUri), anyRequestOptions());
		}
	}

	@Test
	void testDownload_noFileLinkInPage() throws IOException {
		var pageUri = URI.create("https://localhost/downloads/page.html");
		var pageHtml = "No file link";

		var profile = createProfile(Type.STANDARD);
		profile.setPageUrl(pageUri);
		profile.setLinkPattern(Pattern.compile(" href=\"(.+/file-[.0-9]+\\.zip)\""));

		try (var mockWebClient = mock(WebClient.class); var core = new DownloaderCore(mockWebClient)) {
			when(mockWebClient.readString(eq(pageUri), anyRequestOptions())).thenReturn(text(pageHtml));

			core.download(profile);

			verify(mockWebClient).readString(eq(pageUri), anyRequestOptions());
		}
	}

	@Test
	void testDownload_versionInFileName() throws IOException {
		var fileUri = URI.create("https://localhost/downloads/file-1.0.zip");
		var filePath = outDir.resolve("file-1.0.zip");
		var pageUri = URI.create("https://localhost/downloads/page.html");
		var pageHtml = "<a href=\"./file-1.0.zip\">Latest</a>";

		var profile = createProfile(Type.STANDARD);
		profile.setPageUrl(pageUri);
		profile.setLinkPattern(Pattern.compile(" href=\"(.+/file-[.0-9]+\\.zip)\""));

		try (var mockWebClient = mock(WebClient.class); var core = new DownloaderCore(mockWebClient)) {
			when(mockWebClient.readString(eq(pageUri), anyRequestOptions())).thenReturn(text(pageHtml));
			when(mockWebClient.saveToFile(eq(fileUri), anyRequestOptions(), eq(filePath))).thenReturn(fileNotModified());

			core.download(profile);

			assertDownload(mockWebClient, fileUri, filePath);
		}
	}

	@Test
	void testDownload_versionInFileLink() throws IOException {
		var fileUri = URI.create("https://localhost/downloads/1.0/file.zip");
		var filePath = outDir.resolve("file-1.0.zip");
		var pageUri = URI.create("https://localhost/downloads/page.html");
		var pageHtml = "<a href=\"./1.0/file.zip\">Latest</a>";

		var profile = createProfile(Type.STANDARD);
		profile.setPageUrl(pageUri);
		profile.setLinkPattern(Pattern.compile(" href=\"(.+/([.0-9]+)/file\\.zip)\""));

		try (var mockWebClient = mock(WebClient.class); var core = new DownloaderCore(mockWebClient)) {
			when(mockWebClient.readString(eq(pageUri), anyRequestOptions())).thenReturn(text(pageHtml));
			when(mockWebClient.saveToFile(eq(fileUri), anyRequestOptions(), eq(filePath))).thenReturn(fileNotModified());

			core.download(profile);

			assertDownload(mockWebClient, fileUri, filePath);
		}
	}

	@Test
	void testDownload_versionInPage() throws IOException {
		var pageUri = URI.create("https://localhost/downloads/page.html");
		var pageHtml = "<a href=\"./latest/file.zip\">Version 1.0</a>";
		var fileUri = URI.create("https://localhost/downloads/latest/file.zip");
		var filePath = outDir.resolve("file-1.0.zip");

		var profile = createProfile(Type.STANDARD);
		profile.setPageUrl(pageUri);
		profile.setLinkPattern(Pattern.compile(" href=\"(.+/file\\.zip)\""));
		profile.setVersionPattern(Pattern.compile("Version (\\d\\.\\d)"));

		try (var mockWebClient = mock(WebClient.class); var core = new DownloaderCore(mockWebClient)) {
			when(mockWebClient.readString(eq(pageUri), anyRequestOptions())).thenReturn(text(pageHtml));
			when(mockWebClient.saveToFile(eq(fileUri), anyRequestOptions(), eq(filePath))).thenReturn(fileNotModified());

			core.download(profile);

			assertDownload(mockWebClient, fileUri, filePath);
		}
	}

	@Test
	void testDownload_GitHub_noPageFragmentLink() throws IOException {
		var pageUri = URI.create("https://localhost/downloads/page.html");
		var pageHtml = "No file or page fragment link";

		var profile = createProfile(Type.GITHUB);
		profile.setPageUrl(pageUri);
		profile.setLinkPattern(Pattern.compile(" href=\"(.+/file\\.zip)\""));
		profile.setVersionPattern(Pattern.compile("Version (\\d\\.\\d)"));

		try (var mockWebClient = mock(WebClient.class); var core = new DownloaderCore(mockWebClient)) {
			when(mockWebClient.readString(eq(pageUri), anyRequestOptions())).thenReturn(text(pageHtml));

			core.download(profile);

			verify(mockWebClient).readString(eq(pageUri), anyRequestOptions());
		}
	}

	@Test
	void testDownload_GitHub_noPageFragment() throws IOException {
		var pageUri = URI.create("https://localhost/downloads/page.html");
		var pageHtml = "<include-fragment loading=\"lazy\" src=\"https://localhost/account/project/releases/expanded_assets/v1.0\" >";
		var pageFragmentUri = URI.create("https://localhost/account/project/releases/expanded_assets/v1.0");

		var profile = createProfile(Type.GITHUB);
		profile.setPageUrl(pageUri);
		profile.setLinkPattern(Pattern.compile(" href=\"(.+/file\\.zip)\""));
		profile.setVersionPattern(Pattern.compile("Version (\\d\\.\\d)"));

		try (var mockWebClient = mock(WebClient.class); var core = new DownloaderCore(mockWebClient)) {
			when(mockWebClient.readString(eq(pageUri), anyRequestOptions())).thenReturn(text(pageHtml));
			when(mockWebClient.readString(eq(pageFragmentUri), anyRequestOptions())).thenReturn(textNotFound());

			core.download(profile);

			verify(mockWebClient).readString(eq(pageUri), anyRequestOptions());
			verify(mockWebClient).readString(eq(pageFragmentUri), anyRequestOptions());
		}
	}

	@Test
	void testDownload_GitHub_noFileLinkInPageFragment() throws IOException {
		var pageUri = URI.create("https://localhost/downloads/page.html");
		var pageHtml = "<include-fragment loading=\"lazy\" src=\"https://localhost/account/project/releases/expanded_assets/v1.0\" >";
		var pageFragmentUri = URI.create("https://localhost/account/project/releases/expanded_assets/v1.0");
		var pageFragmentHtml = "No link in page fragment";

		var profile = createProfile(Type.GITHUB);
		profile.setPageUrl(pageUri);
		profile.setLinkPattern(Pattern.compile(" href=\"(.+/file\\.zip)\""));
		profile.setVersionPattern(Pattern.compile("Version (\\d\\.\\d)"));

		try (var mockWebClient = mock(WebClient.class); var core = new DownloaderCore(mockWebClient)) {
			when(mockWebClient.readString(eq(pageUri), anyRequestOptions())).thenReturn(text(pageHtml));
			when(mockWebClient.readString(eq(pageFragmentUri), anyRequestOptions())).thenReturn(text(pageFragmentHtml));

			core.download(profile);

			verify(mockWebClient).readString(eq(pageUri), anyRequestOptions());
			verify(mockWebClient).readString(eq(pageFragmentUri), anyRequestOptions());
		}
	}

	@Test
	void testDownload_GitHub() throws IOException {
		var pageUri = URI.create("https://localhost/downloads/page.html");
		var pageHtml = "<include-fragment loading=\"lazy\" src=\"https://localhost/account/project/releases/expanded_assets/v1.0\" >";
		var pageFragmentUri = URI.create("https://localhost/account/project/releases/expanded_assets/v1.0");
		var pageFragmentHtml = "<a href=\"./latest/file.zip\">Version 1.0</a>";
		var fileUri = URI.create("https://localhost/downloads/latest/file.zip");
		var filePath = outDir.resolve("file-1.0.zip");

		var profile = createProfile(Type.GITHUB);
		profile.setPageUrl(pageUri);
		profile.setLinkPattern(Pattern.compile(" href=\"(.+/file\\.zip)\""));
		profile.setVersionPattern(Pattern.compile("Version (\\d\\.\\d)"));

		try (var mockWebClient = mock(WebClient.class); var core = new DownloaderCore(mockWebClient)) {
			when(mockWebClient.readString(eq(pageUri), anyRequestOptions())).thenReturn(text(pageHtml));
			when(mockWebClient.readString(eq(pageFragmentUri), anyRequestOptions())).thenReturn(text(pageFragmentHtml));
			when(mockWebClient.saveToFile(eq(fileUri), anyRequestOptions(), eq(filePath))).thenReturn(fileNotModified());

			core.download(profile);

			assertDownload(mockWebClient, fileUri, filePath);
		}
	}

	@Test
	void testDownload_Mozilla() throws IOException {
		var pageUri = URI.create("https://localhost/pub/firefox/releases/");
		var pageHtml = TestUtils.readResourceAsString("firefox.html");
		var fileUri = URI.create("https://localhost/pub/firefox/releases/78.0.2/win64/en-US/Firefox%20Setup%2078.0.2.exe");
		var filePath = outDir.resolve("Firefox Setup 78.0.2.exe");

		var profile = createProfile(Type.MOZILLA);
		profile.setPageUrl(pageUri);
		profile.setLinkPattern(Pattern.compile("win64/en-US/Firefox"));

		try (var mockWebClient = mock(WebClient.class); var core = new DownloaderCore(mockWebClient)) {
			when(mockWebClient.readString(eq(pageUri), anyRequestOptions())).thenReturn(text(pageHtml));
			when(mockWebClient.saveToFile(eq(fileUri), anyRequestOptions(), eq(filePath))).thenReturn(fileNotModified());

			core.download(profile);

			assertDownload(mockWebClient, fileUri, filePath);
		}
	}

	@Test
	void testPrintResult() throws IOException {
		var fileUri = URI.create("https://localhost/index.html");
		try (var mockWebClient = mock(WebClient.class); var core = new DownloaderCore(mockWebClient)) {
			assertDoesNotThrow(() -> {
				core.printResult(fileUri, new FileResult(Status.OK, Path.of("index.html"), Instant.now()));
				core.printResult(fileUri, fileNotModified());
				core.printResult(fileUri, new FileResult(Status.ERROR, new Exception("Some error")));
			});
		}

	}

}
