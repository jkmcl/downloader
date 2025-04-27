package jkml.downloader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jkml.downloader.http.FileResult;
import jkml.downloader.http.LinkResult;
import jkml.downloader.http.RequestOptions;
import jkml.downloader.http.ResultUtils;
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
		return ResultUtils.textResult(new Exception("Not found"));
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
		return ResultUtils.linkResult(new Exception("No location"));
	}

	private static RequestOptions anyRequestOptions() {
		return nullable(RequestOptions.class);
	}

	private Downloader createDownloaderCore(WebClient webClient) {
		return new Downloader(mock(PrintStream.class), webClient);
	}

	@Test
	void testDownload_fileNotFound() {
		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			assertDoesNotThrow(() -> downloader.download(Path.of("NO_SUCH_FILE.json")));
		}
	}

	@Test
	void testDownload() {
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
	void testDownload_direct() {
		var fileLink = URI.create("https://localhost/downloads/file-1.0.zip");
		var filePath = outDir.resolve("file-1.0.zip");

		var profile = createProfile(Type.DIRECT);
		profile.setFileUrl(fileLink);

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.saveToFile(eq(fileLink), anyRequestOptions(), eq(filePath))).thenReturn(fileNotModified());

			downloader.download(profile);

			assertDownload(mockWebClient, fileLink, filePath);
		}
	}

	@Test
	void testDownload_noPage() {
		var pageLink = URI.create("https://localhost/downloads/page.html");

		var profile = createProfile(Type.STANDARD);
		profile.setPageUrl(pageLink);
		profile.setLinkPattern(Pattern.compile("href=\"([^\"]+/file-[.0-9]+\\.zip)"));

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getContent(eq(pageLink), anyRequestOptions())).thenReturn(textNotFound());

			downloader.download(profile);

			verify(mockWebClient).getContent(eq(pageLink), anyRequestOptions());
		}
	}

	@Test
	void testDownload_noFileLinkInPage() {
		var pageLink = URI.create("https://localhost/downloads/page.html");
		var pageHtml = "No file link";

		var profile = createProfile(Type.STANDARD);
		profile.setPageUrl(pageLink);
		profile.setLinkPattern(Pattern.compile("href=\"([^\"]+/file-[.0-9]+\\.zip)"));

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getContent(eq(pageLink), anyRequestOptions())).thenReturn(text(pageHtml));

			downloader.download(profile);

			verify(mockWebClient).getContent(eq(pageLink), anyRequestOptions());
		}
	}

	@Test
	void testDownload_versionInFileName() {
		var fileLink = URI.create("https://localhost/downloads/file-1.0.zip");
		var filePath = outDir.resolve("file-1.0.zip");
		var pageLink = URI.create("https://localhost/downloads/page.html");
		var pageHtml = "<a href=\"./file-1.0.zip\">Latest</a>";

		var profile = createProfile(Type.STANDARD);
		profile.setPageUrl(pageLink);
		profile.setLinkPattern(Pattern.compile("href=\"([^\"]+/file-[.0-9]+\\.zip)"));

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getContent(eq(pageLink), anyRequestOptions())).thenReturn(text(pageHtml));
			when(mockWebClient.saveToFile(eq(fileLink), anyRequestOptions(), eq(filePath))).thenReturn(fileNotModified());

			downloader.download(profile);

			assertDownload(mockWebClient, fileLink, filePath);
		}
	}

	@Test
	void testDownload_versionInFileLink() {
		var fileLink = URI.create("https://localhost/downloads/1.0/file.zip");
		var filePath = outDir.resolve("file-1.0.zip");
		var pageLink = URI.create("https://localhost/downloads/page.html");
		var pageHtml = "<a href=\"./1.0/file.zip\">Latest</a>";

		var profile = createProfile(Type.STANDARD);
		profile.setPageUrl(pageLink);
		profile.setLinkPattern(Pattern.compile("href=\"([^\"]+/([.0-9]+)/file\\.zip)"));

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getContent(eq(pageLink), anyRequestOptions())).thenReturn(text(pageHtml));
			when(mockWebClient.saveToFile(eq(fileLink), anyRequestOptions(), eq(filePath))).thenReturn(fileNotModified());

			downloader.download(profile);

			assertDownload(mockWebClient, fileLink, filePath);
		}
	}

	@Test
	void testDownload_versionInPage() {
		var pageLink = URI.create("https://localhost/downloads/page.html");
		var pageHtml = "<a href=\"./latest/file.zip\">Version 1.0</a>";
		var fileLink = URI.create("https://localhost/downloads/latest/file.zip");
		var filePath = outDir.resolve("file-1.0.zip");

		var profile = createProfile(Type.STANDARD);
		profile.setPageUrl(pageLink);
		profile.setLinkPattern(Pattern.compile("href=\"([^\"]+/file\\.zip)"));
		profile.setVersionPattern(Pattern.compile("Version (\\d\\.\\d)"));

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getContent(eq(pageLink), anyRequestOptions())).thenReturn(text(pageHtml));
			when(mockWebClient.saveToFile(eq(fileLink), anyRequestOptions(), eq(filePath))).thenReturn(fileNotModified());

			downloader.download(profile);

			assertDownload(mockWebClient, fileLink, filePath);
		}
	}

	@Test
	void testDownload_GitHub_noPageFragmentLink() {
		var pageLink = URI.create("https://localhost/downloads/page.html");
		var pageHtml = "No file or page fragment link";

		var profile = createProfile(Type.GITHUB);
		profile.setPageUrl(pageLink);
		profile.setLinkPattern(Pattern.compile("href=\"([^\"]+/file\\.zip)"));
		profile.setVersionPattern(Pattern.compile("Version (\\d\\.\\d)"));

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getContent(eq(pageLink), anyRequestOptions())).thenReturn(text(pageHtml));

			downloader.download(profile);

			verify(mockWebClient).getContent(eq(pageLink), anyRequestOptions());
		}
	}

	@Test
	void testDownload_GitHub_noPageFragment() {
		var pageLink = URI.create("https://localhost/downloads/page.html");
		var pageHtml = "<include-fragment loading=\"lazy\" src=\"https://localhost/account/project/releases/expanded_assets/v1.0\" >";
		var pageFragmentLink = URI.create("https://localhost/account/project/releases/expanded_assets/v1.0");

		var profile = createProfile(Type.GITHUB);
		profile.setPageUrl(pageLink);
		profile.setLinkPattern(Pattern.compile("href=\"([^\"]+/file\\.zip)"));
		profile.setVersionPattern(Pattern.compile("Version (\\d\\.\\d)"));

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getContent(eq(pageLink), anyRequestOptions())).thenReturn(text(pageHtml));
			when(mockWebClient.getContent(eq(pageFragmentLink), anyRequestOptions())).thenReturn(textNotFound());

			downloader.download(profile);

			verify(mockWebClient).getContent(eq(pageLink), anyRequestOptions());
			verify(mockWebClient).getContent(eq(pageFragmentLink), anyRequestOptions());
		}
	}

	@Test
	void testDownload_GitHub_noFileLinkInPageFragment() {
		var pageLink = URI.create("https://localhost/downloads/page.html");
		var pageHtml = "<include-fragment loading=\"lazy\" src=\"https://localhost/account/project/releases/expanded_assets/v1.0\" >";
		var pageFragmentLink = URI.create("https://localhost/account/project/releases/expanded_assets/v1.0");
		var pageFragmentHtml = "No link in page fragment";

		var profile = createProfile(Type.GITHUB);
		profile.setPageUrl(pageLink);
		profile.setLinkPattern(Pattern.compile("href=\"([^\"]+/file\\.zip)"));
		profile.setVersionPattern(Pattern.compile("Version (\\d\\.\\d)"));

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getContent(eq(pageLink), anyRequestOptions())).thenReturn(text(pageHtml));
			when(mockWebClient.getContent(eq(pageFragmentLink), anyRequestOptions())).thenReturn(text(pageFragmentHtml));

			downloader.download(profile);

			verify(mockWebClient).getContent(eq(pageLink), anyRequestOptions());
			verify(mockWebClient).getContent(eq(pageFragmentLink), anyRequestOptions());
		}
	}

	@Test
	void testDownload_GitHub() {
		var pageLink = URI.create("https://localhost/downloads/page.html");
		var pageHtml = "<include-fragment loading=\"lazy\" src=\"https://localhost/account/project/releases/expanded_assets/v1.0\" >";
		var pageFragmentLink = URI.create("https://localhost/account/project/releases/expanded_assets/v1.0");
		var pageFragmentHtml = "<a href=\"./latest/file.zip\">Version 1.0</a>";
		var fileLink = URI.create("https://localhost/downloads/latest/file.zip");
		var filePath = outDir.resolve("file-1.0.zip");

		var profile = createProfile(Type.GITHUB);
		profile.setPageUrl(pageLink);
		profile.setLinkPattern(Pattern.compile("href=\"([^\"]+/file\\.zip)"));
		profile.setVersionPattern(Pattern.compile("Version (\\d\\.\\d)"));

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getContent(eq(pageLink), anyRequestOptions())).thenReturn(text(pageHtml));
			when(mockWebClient.getContent(eq(pageFragmentLink), anyRequestOptions())).thenReturn(text(pageFragmentHtml));
			when(mockWebClient.saveToFile(eq(fileLink), anyRequestOptions(), eq(filePath))).thenReturn(fileNotModified());

			downloader.download(profile);

			assertDownload(mockWebClient, fileLink, filePath);
		}
	}

	@Test
	void testDownload_redirect_noLink() {
		var redirectLink = URI.create("https://localhost/downloads/api");

		var profile = createProfile(Type.REDIRECT);
		profile.setFileUrl(redirectLink);

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getLocation(eq(redirectLink), anyRequestOptions())).thenReturn(nolink());

			downloader.download(profile);

			verify(mockWebClient).getLocation(eq(redirectLink), anyRequestOptions());
		}
	}

	@Test
	void testDownload_redirect() {
		var redirectLink = URI.create("https://localhost/downloads/api");
		var fileLink = URI.create("https://localhost/downloads/file-1.0.zip");
		var filePath = outDir.resolve("file-1.0.zip");

		var profile = createProfile(Type.REDIRECT);
		profile.setFileUrl(redirectLink);

		try (var mockWebClient = mock(WebClient.class); var downloader = createDownloaderCore(mockWebClient)) {
			when(mockWebClient.getLocation(eq(redirectLink), anyRequestOptions())).thenReturn(link(fileLink));
			when(mockWebClient.saveToFile(eq(fileLink), anyRequestOptions(), eq(filePath))).thenReturn(fileNotModified());

			downloader.download(profile);

			assertDownload(mockWebClient, fileLink, filePath);
		}
	}

	@Test
	void testMain_noArg() {
		assertDoesNotThrow((Executable) Downloader::main);
	}

	@Test
	void testMain() {
		assertDoesNotThrow(() -> Downloader.main("src/test/resources/profiles-empty.json"));
	}

}
