package jkml.downloader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.stubbing.OngoingStubbing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jkml.downloader.html.FileInfo;
import jkml.downloader.http.FileResult;
import jkml.downloader.http.RequestOptions;
import jkml.downloader.http.WebClient;
import jkml.downloader.http.WebClientException;
import jkml.downloader.profile.Profile;
import jkml.downloader.profile.Profile.Type;
import jkml.downloader.util.FileUtils;
import jkml.downloader.util.StringUtils;
import jkml.downloader.util.TestUtils;

class DownloaderTests {

	private static final URI PAGE_LINK = URI.create("https://localhost/downloads/page.html");

	private static final URI API_LINK = URI.create("https://localhost/downloads/api");

	private static final URI FILE_LINK = URI.create("https://localhost/downloads/file-1.0.zip");

	private static final Logger logger = LoggerFactory.getLogger(DownloaderTests.class);

	private static final Path inDir = TestUtils.resourcesDirectory();

	@TempDir
	static Path tempDir;

	private static class ProfileBuilder {

		private final Type type;

		private URI fileUrl;

		private URI pageUrl;

		private String linkPattern;

		private String versionPattern;

		ProfileBuilder(Type type) {
			this.type = type;
		}

		ProfileBuilder fileUrl(URI value) {
			fileUrl = value;
			return this;
		}

		ProfileBuilder pageUrl(URI value) {
			pageUrl = value;
			return this;
		}

		ProfileBuilder linkPattern(String value) {
			linkPattern = value;
			return this;
		}

		ProfileBuilder versionPattern(String value) {
			versionPattern = value;
			return this;
		}

		Profile build() {
			var profile = new Profile();
			profile.setName("Something");
			profile.setType(type);
			profile.setOutputDirectory(tempDir);
			if (fileUrl != null) {
				profile.setFileUrl(fileUrl);
			}
			if (pageUrl != null) {
				profile.setPageUrl(pageUrl);
			}
			if (linkPattern != null) {
				profile.setLinkPattern(Pattern.compile(linkPattern));
			}
			if (versionPattern != null) {
				profile.setVersionPattern(Pattern.compile(versionPattern));
			}
			return profile;
		}

	}

	@BeforeEach
	void beforeEach(TestInfo testInfo) {
		logger.info("# Executing {}", testInfo.getDisplayName());
	}

	@AfterEach
	void afterEach() {
		logger.info(StringUtils.EMPTY);
	}

	private static RequestOptions anyRequestOptions() {
		return nullable(RequestOptions.class);
	}

	private static FileResult file() {
		return new FileResult(Instant.now());
	}

	private static FileResult fileNotModified() {
		return new FileResult();
	}

	private static Throwable exception() {
		return new WebClientException("Mock exception");
	}

	@Test
	void testLoadProfiles() {
		try (var downloader = new Downloader()) {
			assertFalse(downloader.loadProfiles(inDir.resolve("profiles.json")).isEmpty());
		}
	}

	@Test
	void testLoadProfiles_noSuchFile() {
		try (var downloader = new Downloader()) {
			assertTrue(downloader.loadProfiles(Path.of("no_such_file.json")).isEmpty());
		}
	}

	@Test
	void testLoadProfiles_invalidFile() {
		try (var downloader = new Downloader()) {
			assertTrue(downloader.loadProfiles(inDir.resolve("profiles-error.json")).isEmpty());
		}
	}

	@Test
	void testDownload() throws Exception {
		try (var mockWebClient = mock(WebClient.class); var downloader = new Downloader(mockWebClient)) {
			when(mockWebClient.getLocation(any(URI.class), anyRequestOptions())).thenReturn(FILE_LINK);
			when(mockWebClient.getContent(any(URI.class), anyRequestOptions())).thenReturn("");
			when(mockWebClient.saveToFile(any(URI.class), anyRequestOptions(), any(Path.class))).thenReturn(file());

			downloader.download(inDir.resolve("profiles.json"));

			verify(mockWebClient).getLocation(any(URI.class), anyRequestOptions());
			verify(mockWebClient, times(3)).getContent(any(URI.class), anyRequestOptions());
			verify(mockWebClient, times(2)).saveToFile(any(URI.class), anyRequestOptions(), any(Path.class));
		}
	}

	private static Downloadable testFindInPage(Consumer<OngoingStubbing<String>> whenGetContent) throws Exception {
		var profile = new ProfileBuilder(Type.STANDARD).pageUrl(PAGE_LINK).linkPattern(FILE_LINK_VERSION_IN_NAME_RE)
				.build();

		try (var mockWebClient = mock(WebClient.class); var downloader = new Downloader(mockWebClient)) {
			var stubbing = when(mockWebClient.getContent(eq(PAGE_LINK), anyRequestOptions()));
			whenGetContent.accept(stubbing);
			return downloader.findInPage(profile);
		}
	}

	@Test
	void testFindInPage() throws Exception {
		var result = testFindInPage(w -> w.thenReturn(FILE_LINK_VERSION_IN_NAME_HTML));
		assertEquals(FILE_LINK, result.uri());
		assertEquals(FileUtils.getFileName(FILE_LINK), result.fileName());
	}

	@Test
	void testFindInPage_notFound() throws Exception {
		var result = testFindInPage(w -> w.thenReturn(NO_LINK_HTML));
		assertNull(result);
	}

	private static final String VERSION = "1.0";

	private static final URI FILE_LINK_NO_VERSION = URI.create("https://localhost/downloads/latest/file.zip");

	private static final URI FILE_LINK_VERSION_IN_NAME = URI.create("https://localhost/downloads/file-1.0.zip");

	private static final URI FILE_LINK_VERSION_IN_PARENT = URI.create("https://localhost/downloads/1.0/file.zip");

	private static final URI PAGE_FRAGMENT_LINK = URI.create("https://localhost/account/project/releases/expanded_assets/v1.0");

	private static final String NO_LINK_HTML = """
			No link or version""";

	private static final String FILE_LINK_NO_VERSION_HTML = """
			<a href="./latest/file.zip">Version 1.0</a>""";

	private static final String FILE_LINK_VERSION_IN_NAME_HTML = """
			<a href="./file-1.0.zip">Latest</a>""";

	private static final String FILE_LINK_VERSION_IN_PARENT_HTML = """
			<a href="./1.0/file.zip">Latest</a>""";

	private static final String PAGE_FRAGMENT_LINK_HTML = """
			<include-fragment loading="lazy" src="https://localhost/account/project/releases/expanded_assets/v1.0" >
			""";

	private static final String VERSION_RE = """
			Version (\\d\\.\\d)""";

	private static final String FILE_LINK_NO_VERSION_RE = """
			href="([^"]+/file\\.zip)""";

	private static final String FILE_LINK_VERSION_IN_NAME_RE = """
			href="([^"]+/file-[.0-9]+\\.zip)""";

	private static final String FILE_LINK_VERSION_IN_PARENT_RE = """
			href="([^"]+/([.0-9]+)/file\\.zip)""";

	private static class FindFileInfoTester {

		final Map<URI, Consumer<OngoingStubbing<String>>> stubbings = new LinkedHashMap<>();

		final ProfileBuilder profileBuilder;

		FindFileInfoTester(Type type, String linkPattern) {
			profileBuilder = new ProfileBuilder(type).pageUrl(PAGE_LINK).linkPattern(linkPattern);
		}

		void test(FileInfo expected) throws Exception {
			var profile = profileBuilder.build();
			try (var mockWebClient = mock(WebClient.class); var downloader = new Downloader(mockWebClient)) {
				for (var entry : stubbings.entrySet()) {
					var uri = entry.getKey();
					var stubbing = entry.getValue();
					stubbing.accept(when(mockWebClient.getContent(eq(uri), anyRequestOptions())));
				}
				var result = downloader.findFileInfo(profile);

				if (expected == null) {
					assertNull(result);
				} else {
					assertEquals(expected.uri(), result.uri());
					assertEquals(expected.version(), result.version());
				}
			}
		}

	}

	@Test
	void testFindFileInfo_versionInName() throws Exception {
		var expected = new FileInfo(FILE_LINK_VERSION_IN_NAME, null);

		var tester = new FindFileInfoTester(Type.STANDARD, FILE_LINK_VERSION_IN_NAME_RE);
		tester.stubbings.put(PAGE_LINK, w -> w.thenReturn(FILE_LINK_VERSION_IN_NAME_HTML));
		tester.test(expected);
	}

	@Test
	void testFindFileInfo_versionInParent() throws Exception {
		var expected = new FileInfo(FILE_LINK_VERSION_IN_PARENT, VERSION);

		var tester = new FindFileInfoTester(Type.STANDARD, FILE_LINK_VERSION_IN_PARENT_RE);
		tester.stubbings.put(PAGE_LINK, w -> w.thenReturn(FILE_LINK_VERSION_IN_PARENT_HTML));
		tester.test(expected);
	}

	@Test
	void testFindFileInfo_versionInPage() throws Exception {
		var expected = new FileInfo(FILE_LINK_NO_VERSION, VERSION);

		var tester = new FindFileInfoTester(Type.STANDARD, FILE_LINK_NO_VERSION_RE);
		tester.profileBuilder.versionPattern(VERSION_RE);
		tester.stubbings.put(PAGE_LINK, w -> w.thenReturn(FILE_LINK_NO_VERSION_HTML));
		tester.test(expected);
	}

	@Test
	void testFindFileInfo_GitHub_noPageFragmentLink() throws Exception {
		var expected = (FileInfo) null;

		var tester = new FindFileInfoTester(Type.GITHUB, FILE_LINK_NO_VERSION_RE);
		tester.profileBuilder.versionPattern(VERSION_RE);
		tester.stubbings.put(PAGE_LINK, w -> w.thenReturn(NO_LINK_HTML));
		tester.test(expected);
	}

	@Test
	void testFindFileInfo_GitHub_noPageFragment() throws Exception {
		var expected = (FileInfo) null;

		var tester = new FindFileInfoTester(Type.GITHUB, FILE_LINK_NO_VERSION_RE);
		tester.profileBuilder.versionPattern(VERSION_RE);
		tester.stubbings.put(PAGE_LINK, w -> w.thenReturn(PAGE_FRAGMENT_LINK_HTML));
		tester.stubbings.put(PAGE_FRAGMENT_LINK, w -> w.thenThrow(exception()));
		tester.test(expected);
	}

	@Test
	void testFindFileInfo_GitHub_noFileLinkInPageFragment() throws Exception {
		var expected = (FileInfo) null;

		var tester = new FindFileInfoTester(Type.GITHUB, FILE_LINK_NO_VERSION_RE);
		tester.profileBuilder.versionPattern(VERSION_RE);
		tester.stubbings.put(PAGE_LINK, w -> w.thenReturn(PAGE_FRAGMENT_LINK_HTML));
		tester.stubbings.put(PAGE_FRAGMENT_LINK, w -> w.thenReturn(NO_LINK_HTML));
		tester.test(expected);
	}

	@Test
	void testFindFileInfo_GitHub() throws Exception {
		var expected = new FileInfo(FILE_LINK_NO_VERSION, VERSION);

		var tester = new FindFileInfoTester(Type.GITHUB, FILE_LINK_NO_VERSION_RE);
		tester.profileBuilder.versionPattern(VERSION_RE);
		tester.stubbings.put(PAGE_LINK, w -> w.thenReturn(PAGE_FRAGMENT_LINK_HTML));
		tester.stubbings.put(PAGE_FRAGMENT_LINK, w -> w.thenReturn(FILE_LINK_NO_VERSION_HTML));
		tester.test(expected);
	}

	private static Downloadable testFindInHeader(Consumer<OngoingStubbing<URI>> whenGetLocation) throws Exception {
		try (var mockWebClient = mock(WebClient.class); var downloader = new Downloader(mockWebClient)) {
			var stubbing = when(mockWebClient.getLocation(eq(API_LINK), anyRequestOptions()));
			whenGetLocation.accept(stubbing);
			return downloader.findInHeader(API_LINK, new RequestOptions());
		}
	}

	@Test
	void testFindInHeader() throws Exception {
		var result = testFindInHeader(w -> w.thenReturn(FILE_LINK));
		assertEquals(FILE_LINK, result.uri());
		assertEquals(FileUtils.getFileName(FILE_LINK), result.fileName());
	}

	@Test
	void testFindInHeader_notFound() throws Exception {
		var result = testFindInHeader(w -> w.thenReturn(null));
		assertNull(result);
	}

	@Test
	void testFindInHeader_exception() throws Exception {
		var result = testFindInHeader(w -> w.thenThrow(exception()));
		assertNull(result);
	}

	private static class DownloadTester {

		final URI fileLink;

		final Profile profile;

		boolean fileExits;

		Downloadable downloadable;

		Consumer<OngoingStubbing<FileResult>> whenSaveToFile = w -> w.thenReturn(file());

		DownloadTester(URI fileLink) {
			this.fileLink = fileLink;
			profile = new ProfileBuilder(Type.DIRECT).fileUrl(fileLink).build();
			downloadable = new Downloadable(fileLink);
		}

		void test(boolean expectWebClientInvoked) throws Exception {
			var filePath = profile.getOutputDirectory().resolve(FileUtils.getFileName(fileLink));
			if (fileExits) {
				TestUtils.createFile(filePath);
			} else {
				Files.deleteIfExists(filePath);
			}

			try (var mockWebClient = mock(WebClient.class); var downloader = new Downloader(mockWebClient)) {
				var stubbing = when(mockWebClient.saveToFile(eq(fileLink), anyRequestOptions(), eq(filePath)));
				whenSaveToFile.accept(stubbing);

				downloader.download(profile, downloadable);

				if (expectWebClientInvoked) {
					verify(mockWebClient).saveToFile(eq(fileLink), anyRequestOptions(), eq(filePath));
				} else {
					verifyNoInteractions(mockWebClient);
				}
			}
		}

	}

	@Test
	void testDownload_noDownloadable() throws Exception {
		var tester = new DownloadTester(FILE_LINK);
		tester.downloadable = null;
		tester.test(false);
	}

	@Test
	void testDownload_noDownloadableFileName() throws Exception {
		var tester = new DownloadTester(FILE_LINK);
		tester.downloadable = new Downloadable(FILE_LINK, null);
		tester.test(false);
	}

	@Test
	void testDownload_skipIfFileExists_fileExists() throws Exception {
		var tester = new DownloadTester(FILE_LINK);
		tester.profile.setSkipIfFileExists(true);
		tester.fileExits = true;
		tester.test(false);
	}

	@Test
	void testDownload_ok_skipIfFileExists() throws Exception {
		var tester = new DownloadTester(FILE_LINK);
		tester.profile.setSkipIfFileExists(true);
		tester.test(true);
	}

	@Test
	void testDownload_ok_fileExists() throws Exception {
		var tester = new DownloadTester(FILE_LINK);
		tester.fileExits = true;
		tester.test(true);
	}

	@Test
	void testDownload_ok() throws Exception {
		var tester = new DownloadTester(FILE_LINK);
		tester.test(true);
	}

	@Test
	void testDownload_exception() throws Exception {
		var tester = new DownloadTester(FILE_LINK);
		tester.whenSaveToFile = w -> w.thenThrow(exception());
		tester.test(true);
	}

	@Test
	void testDownload_notModified() throws Exception {
		var tester = new DownloadTester(FILE_LINK);
		tester.whenSaveToFile = w -> w.thenReturn(fileNotModified());
		tester.test(true);
	}

}
