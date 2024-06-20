package jkml.downloader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintStream;
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
import jkml.downloader.http.LinkResult;
import jkml.downloader.http.RequestOptions;
import jkml.downloader.http.TextResult;
import jkml.downloader.http.WebClient;
import jkml.downloader.profile.Profile;
import jkml.downloader.profile.Profile.Type;
import jkml.downloader.util.StringUtils;
import jkml.downloader.util.TestUtils;

class DownloaderTests {

	private static final String PROFILES_JSON_FILE_NAME = "profiles.json";

	private static final Logger logger = LoggerFactory.getLogger(DownloaderTests.class);

	private static final Path outDir = TestUtils.outputDirectory();

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
		return new TextResult(text);
	}

	private static TextResult textNotFound() {
		return new TextResult(new Exception("Not found"));
	}

	private static FileResult file() {
		return new FileResult(Instant.now());
	}

	private static FileResult fileNotModified() {
		return new FileResult();
	}

	private static LinkResult link(URI link) {
		return new LinkResult(link);
	}

	private static LinkResult nolink() {
		return new LinkResult(new Exception("No location"));
	}

	private static RequestOptions anyRequestOptions() {
		return nullable(RequestOptions.class);
	}

	private Downloader createDownloaderCore(WebClient webClient) {
		return new Downloader(mock(PrintStream.class), webClient);
	}

	@Test
	void testDownload_fileNotFound() throws IOException {
		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			assertDoesNotThrow(() -> downloader.download(Path.of("NO_SUCH_FILE.json")));
		}
	}

	@Test
	void testDownload() throws IOException {
		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getLocation(any(URI.class), anyRequestOptions())).thenReturn(link(URI.create("http://localhost/")));
			when(mockWebClient.getContent(any(URI.class), anyRequestOptions())).thenReturn(text(""));
			when(mockWebClient.saveToFile(any(URI.class), anyRequestOptions(), any(Path.class))).thenReturn(file());

			downloader.download(TestUtils.getResoureAsPath(PROFILES_JSON_FILE_NAME));

			verify(mockWebClient).getLocation(any(URI.class), anyRequestOptions());
			verify(mockWebClient, times(3)).getContent(any(URI.class), anyRequestOptions());
			verify(mockWebClient, times(2)).saveToFile(any(URI.class), anyRequestOptions(), any(Path.class));
		}
	}

	private static void assertDownload(WebClient mock, URI uri, Path path) {
		verify(mock).saveToFile(eq(uri), anyRequestOptions(), eq(path));
	}

	@Test
	void testDownload_direct() throws IOException {
		var fileUri = URI.create("https://localhost/downloads/file-1.0.zip");
		var filePath = outDir.resolve("file-1.0.zip");

		var profile = createProfile(Type.DIRECT);
		profile.setFileUrl(fileUri);

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.saveToFile(eq(fileUri), anyRequestOptions(), eq(filePath))).thenReturn(fileNotModified());

			downloader.download(profile);

			assertDownload(mockWebClient, fileUri, filePath);
		}
	}

	@Test
	void testDownload_noPage() throws IOException {
		var pageUri = URI.create("https://localhost/downloads/page.html");

		var profile = createProfile(Type.STANDARD);
		profile.setPageUrl(pageUri);
		profile.setLinkPattern(Pattern.compile("href=\"([^\"]+/file-[.0-9]+\\.zip)"));

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getContent(eq(pageUri), anyRequestOptions())).thenReturn(textNotFound());

			downloader.download(profile);

			verify(mockWebClient).getContent(eq(pageUri), anyRequestOptions());
		}
	}

	@Test
	void testDownload_noFileLinkInPage() throws IOException {
		var pageUri = URI.create("https://localhost/downloads/page.html");
		var pageHtml = "No file link";

		var profile = createProfile(Type.STANDARD);
		profile.setPageUrl(pageUri);
		profile.setLinkPattern(Pattern.compile("href=\"([^\"]+/file-[.0-9]+\\.zip)"));

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getContent(eq(pageUri), anyRequestOptions())).thenReturn(text(pageHtml));

			downloader.download(profile);

			verify(mockWebClient).getContent(eq(pageUri), anyRequestOptions());
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
		profile.setLinkPattern(Pattern.compile("href=\"([^\"]+/file-[.0-9]+\\.zip)"));

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getContent(eq(pageUri), anyRequestOptions())).thenReturn(text(pageHtml));
			when(mockWebClient.saveToFile(eq(fileUri), anyRequestOptions(), eq(filePath))).thenReturn(fileNotModified());

			downloader.download(profile);

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
		profile.setLinkPattern(Pattern.compile("href=\"([^\"]+/([.0-9]+)/file\\.zip)"));

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getContent(eq(pageUri), anyRequestOptions())).thenReturn(text(pageHtml));
			when(mockWebClient.saveToFile(eq(fileUri), anyRequestOptions(), eq(filePath))).thenReturn(fileNotModified());

			downloader.download(profile);

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
		profile.setLinkPattern(Pattern.compile("href=\"([^\"]+/file\\.zip)"));
		profile.setVersionPattern(Pattern.compile("Version (\\d\\.\\d)"));

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getContent(eq(pageUri), anyRequestOptions())).thenReturn(text(pageHtml));
			when(mockWebClient.saveToFile(eq(fileUri), anyRequestOptions(), eq(filePath))).thenReturn(fileNotModified());

			downloader.download(profile);

			assertDownload(mockWebClient, fileUri, filePath);
		}
	}

	@Test
	void testDownload_GitHub_noPageFragmentLink() throws IOException {
		var pageUri = URI.create("https://localhost/downloads/page.html");
		var pageHtml = "No file or page fragment link";

		var profile = createProfile(Type.GITHUB);
		profile.setPageUrl(pageUri);
		profile.setLinkPattern(Pattern.compile("href=\"([^\"]+/file\\.zip)"));
		profile.setVersionPattern(Pattern.compile("Version (\\d\\.\\d)"));

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getContent(eq(pageUri), anyRequestOptions())).thenReturn(text(pageHtml));

			downloader.download(profile);

			verify(mockWebClient).getContent(eq(pageUri), anyRequestOptions());
		}
	}

	@Test
	void testDownload_GitHub_noPageFragment() throws IOException {
		var pageUri = URI.create("https://localhost/downloads/page.html");
		var pageHtml = "<include-fragment loading=\"lazy\" src=\"https://localhost/account/project/releases/expanded_assets/v1.0\" >";
		var pageFragmentUri = URI.create("https://localhost/account/project/releases/expanded_assets/v1.0");

		var profile = createProfile(Type.GITHUB);
		profile.setPageUrl(pageUri);
		profile.setLinkPattern(Pattern.compile("href=\"([^\"]+/file\\.zip)"));
		profile.setVersionPattern(Pattern.compile("Version (\\d\\.\\d)"));

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getContent(eq(pageUri), anyRequestOptions())).thenReturn(text(pageHtml));
			when(mockWebClient.getContent(eq(pageFragmentUri), anyRequestOptions())).thenReturn(textNotFound());

			downloader.download(profile);

			verify(mockWebClient).getContent(eq(pageUri), anyRequestOptions());
			verify(mockWebClient).getContent(eq(pageFragmentUri), anyRequestOptions());
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
		profile.setLinkPattern(Pattern.compile("href=\"([^\"]+/file\\.zip)"));
		profile.setVersionPattern(Pattern.compile("Version (\\d\\.\\d)"));

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getContent(eq(pageUri), anyRequestOptions())).thenReturn(text(pageHtml));
			when(mockWebClient.getContent(eq(pageFragmentUri), anyRequestOptions())).thenReturn(text(pageFragmentHtml));

			downloader.download(profile);

			verify(mockWebClient).getContent(eq(pageUri), anyRequestOptions());
			verify(mockWebClient).getContent(eq(pageFragmentUri), anyRequestOptions());
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
		profile.setLinkPattern(Pattern.compile("href=\"([^\"]+/file\\.zip)"));
		profile.setVersionPattern(Pattern.compile("Version (\\d\\.\\d)"));

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getContent(eq(pageUri), anyRequestOptions())).thenReturn(text(pageHtml));
			when(mockWebClient.getContent(eq(pageFragmentUri), anyRequestOptions())).thenReturn(text(pageFragmentHtml));
			when(mockWebClient.saveToFile(eq(fileUri), anyRequestOptions(), eq(filePath))).thenReturn(fileNotModified());

			downloader.download(profile);

			assertDownload(mockWebClient, fileUri, filePath);
		}
	}

	@Test
	void testDownload_redirect_noLink() throws IOException {
		var apiUri = URI.create("https://localhost/downloads/api");

		var profile = createProfile(Type.REDIRECT);
		profile.setFileUrl(apiUri);

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getLocation(eq(apiUri), anyRequestOptions())).thenReturn(nolink());

			downloader.download(profile);

			verify(mockWebClient).getLocation(eq(apiUri), anyRequestOptions());
		}
	}

	@Test
	void testDownload_redirect() throws IOException {
		var apiUri = URI.create("https://localhost/downloads/api");
		var fileUri = URI.create("https://localhost/downloads/file-1.0.zip");
		var filePath = outDir.resolve("file-1.0.zip");

		var profile = createProfile(Type.REDIRECT);
		profile.setFileUrl(apiUri);

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getLocation(eq(apiUri), anyRequestOptions())).thenReturn(link(fileUri));
			when(mockWebClient.saveToFile(eq(fileUri), anyRequestOptions(), eq(filePath))).thenReturn(fileNotModified());

			downloader.download(profile);

			assertDownload(mockWebClient, fileUri, filePath);
		}
	}

	@Test
	void testMain_noArg() {
		assertDoesNotThrow(() -> Downloader.main());
	}

	@Test
	void testMain() {
		assertDoesNotThrow(() -> Downloader.main("src/test/resources/profiles-empty.json"));
	}

}
