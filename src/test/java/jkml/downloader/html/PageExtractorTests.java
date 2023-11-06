package jkml.downloader.html;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jkml.downloader.util.TestUtils;

class PageExtractorTests {

	private final Logger logger = LoggerFactory.getLogger(PageExtractorTests.class);

	@BeforeEach
	void beforeEach(TestInfo testInfo) {
		logger.atInfo().log("# Executing {}", testInfo.getDisplayName());
	}

	@AfterEach
	void afterEach() {
		logger.atInfo().log();
	}

	@Test
	void testExtractFileInfo() {
		var baseUri = URI.create("https://localhost/dir1");
		var html = "<a href=\"dir2/v1.0/file.txt\">File v2.0</a>\n<a href=\"dir4/v3.0/other.txt\">Other v4.0</a>";
		var extr = new PageExtractor(baseUri, html);
		FileInfo fileInfo;

		// Link not found (no match)
		fileInfo = extr.extractFileInfo(Pattern.compile("href=\"(.+/not_exist\\.txt)\""), Occurrence.FIRST);
		assertNull(fileInfo);

		// Link not found (no capturing group)
		fileInfo = extr.extractFileInfo(Pattern.compile("href=\".+/file\\.txt\""), Occurrence.FIRST);
		assertNull(fileInfo);

		// Link only
		fileInfo = extr.extractFileInfo(Pattern.compile("href=\"(.+/file\\.txt)\""), Occurrence.FIRST);
		assertEquals("https://localhost/dir2/v1.0/file.txt", fileInfo.uri().toString());
		assertNull(fileInfo.version());

		// Link only (last occurrence)
		fileInfo = extr.extractFileInfo(Pattern.compile("href=\"(.+/other\\.txt)\""), Occurrence.LAST);
		assertEquals("https://localhost/dir4/v3.0/other.txt", fileInfo.uri().toString());
		assertNull(fileInfo.version());

		// Link and version in link
		fileInfo = extr.extractFileInfo(Pattern.compile("href=\"(.+/v([.0-9]+)/file\\.txt)\""), Occurrence.FIRST);
		assertEquals("https://localhost/dir2/v1.0/file.txt", fileInfo.uri().toString());
		assertEquals("1.0", fileInfo.version());
	}

	@Test
	void testExtractFileInfo2() {
		var baseUri = URI.create("https://localhost/dir1");
		var html = "<a href=\"dir2/v1.0/file.txt\">File v2.0</a>\n<a href=\"dir4/v3.0/other.txt\">Other v4.0</a>";
		var extr = new PageExtractor(baseUri, html);
		FileInfo fileInfo;

		// Link not found (no match)
		fileInfo = extr.extractFileInfo(Pattern.compile("href=\"(.+/not_exist\\.txt)\""), Occurrence.FIRST, null);
		assertNull(fileInfo);

		// Link found but no version pattern
		fileInfo = extr.extractFileInfo(Pattern.compile("href=\"(.+/file\\.txt)\""), Occurrence.FIRST, null);
		assertEquals("https://localhost/dir2/v1.0/file.txt", fileInfo.uri().toString());
		assertNull(fileInfo.version());

		// Link and version in page
		fileInfo = extr.extractFileInfo(Pattern.compile("href=\"(.+/file\\.txt)\""), Occurrence.FIRST, Pattern.compile(">File v([.0-9]+)<"));
		assertEquals("https://localhost/dir2/v1.0/file.txt", fileInfo.uri().toString());
		assertEquals("2.0", fileInfo.version());
	}

	@Test
	void testResolve() {
		var baseStr = "https://localhost";
		var baseUri = URI.create(baseStr);
		var extractor = new PageExtractor(baseUri, null);
		var pathStr = "/expected";
		var expected = baseUri.resolve(pathStr);
		assertEquals(expected, extractor.resolve(baseStr + pathStr));
		assertEquals(expected, extractor.resolve(pathStr));
	}

	@Test
	void testExtractVersion() {
		var extr = new PageExtractor(URI.create("https://localhost/"), "<a>Exist 1.0</a>");
		assertEquals("1.0", extr.extractVersion(Pattern.compile(">Exist ([.0-9]+)<")));
		assertEquals(null, extr.extractVersion(Pattern.compile(">NotExist ([.0-9]+)<")));
	}

	@Test
	void testExtractMozillaFileInfo() {
		var baseUri = URI.create("https://download-installer.cdn.mozilla.net/pub/firefox/releases/");
		var html = "";
		var osLangProduct = "win64/en-US/Firefox";
		var extr = new PageExtractor(baseUri, html);
		assertNull(extr.extractMozillaFileInfo(osLangProduct));
	}

	@Test
	void testExtractMozillaFileInfo_Firefox() {
		var baseStr = "https://download-installer.cdn.mozilla.net/pub/firefox/releases/";
		var baseUri = URI.create(baseStr);
		var html = TestUtils.readResourceAsString("firefox.html");
		var osLangProduct = "win64/en-US/Firefox";
		var extr = new PageExtractor(baseUri, html);
		var fileInfo = extr.extractMozillaFileInfo(osLangProduct);
		assertEquals(baseStr + "78.0.2/win64/en-US/Firefox%20Setup%2078.0.2.exe", fileInfo.uri().toString());
	}

	@Test
	void testExtractMozillaFileInfo_Thunderbird() {
		var baseStr = "https://download-installer.cdn.mozilla.net/pub/thunderbird/releases/";
		var baseUri = URI.create(baseStr);
		var html = TestUtils.readResourceAsString("thunderbird.html");
		var osLangProduct = "win64/en-US/Thunderbird";
		var extr = new PageExtractor(baseUri, html);
		var fileInfo = extr.extractMozillaFileInfo(osLangProduct);
		assertEquals(baseStr + "78.0/win64/en-US/Thunderbird%20Setup%2078.0.exe", fileInfo.uri().toString());
	}

	@Test
	void testExtractGitHubPageFragmentUriList() {
		var baseUri = URI.create("https://github.com/google/guetzli/releases");
		var html = "<include-fragment loading=\"lazy\" src=\"https://github.com/google/guetzli/releases/expanded_assets/v1.0.1\" >"
				+ "<include-fragment loading=\"lazy\" src=\"https://github.com/google/guetzli/releases/expanded_assets/v1.0\" >";
		var extr = new PageExtractor(baseUri, html);
		var actual = extr.extractGitHubPageFragmentUriList();

		assertEquals(2, actual.size());
		assertEquals("https://github.com/google/guetzli/releases/expanded_assets/v1.0.1", actual.get(0).toString());
		assertEquals("https://github.com/google/guetzli/releases/expanded_assets/v1.0", actual.get(1).toString());
	}

}
