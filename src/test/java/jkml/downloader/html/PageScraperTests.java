package jkml.downloader.html;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jkml.downloader.util.StringUtils;

class PageScraperTests {

	private static final Logger logger = LoggerFactory.getLogger(PageScraperTests.class);

	@BeforeEach
	void beforeEach(TestInfo testInfo) {
		logger.info("# Executing {}", testInfo.getDisplayName());
	}

	@AfterEach
	void afterEach() {
		logger.info(StringUtils.EMPTY);
	}

	@Test
	void testExtractFileInfo() {
		var baseUri = URI.create("https://localhost/dir1");
		var html = "<a href=\"dir2/v1.0/file.txt\">File v2.0</a>\n<a href=\"dir4/v3.0/other.txt\">Other v4.0</a>";
		var scraper = new PageScraper(baseUri, html);
		FileInfo fileInfo = null;

		// Link not found (no match)
		fileInfo = scraper.extractFileInfo(Pattern.compile("href=\"([^\"]+/not_exist\\.txt)"), Occurrence.FIRST, null);
		assertNull(fileInfo);

		// Link not found (no capturing group)
		fileInfo = scraper.extractFileInfo(Pattern.compile("href=\"[^\"]+/file\\.txt"), Occurrence.FIRST, null);
		assertNull(fileInfo);

		// Link only
		fileInfo = scraper.extractFileInfo(Pattern.compile("href=\"([^\"]+/file\\.txt)"), Occurrence.FIRST, null);
		assertEquals("https://localhost/dir2/v1.0/file.txt", fileInfo.uri().toString());
		assertNull(fileInfo.version());

		// Link only (null occurrence)
		fileInfo = scraper.extractFileInfo(Pattern.compile("href=\"([^\"]+/file\\.txt)"), null, null);
		assertEquals("https://localhost/dir2/v1.0/file.txt", fileInfo.uri().toString());
		assertNull(fileInfo.version());

		// Link only (last occurrence)
		fileInfo = scraper.extractFileInfo(Pattern.compile("href=\"([^\"]+/other\\.txt)"), Occurrence.LAST, null);
		assertEquals("https://localhost/dir4/v3.0/other.txt", fileInfo.uri().toString());
		assertNull(fileInfo.version());

		// Link and version
		fileInfo = scraper.extractFileInfo(Pattern.compile("href=\"([^\"]+/v([.0-9]+)/file\\.txt)"), Occurrence.FIRST, null);
		assertEquals("https://localhost/dir2/v1.0/file.txt", fileInfo.uri().toString());
		assertEquals("1.0", fileInfo.version());

		// Link and version in page
		fileInfo = scraper.extractFileInfo(Pattern.compile("href=\"([^\"]+/file\\.txt)"), Occurrence.FIRST, Pattern.compile(">File v([.0-9]+)<"));
		assertEquals("https://localhost/dir2/v1.0/file.txt", fileInfo.uri().toString());
		assertEquals("2.0", fileInfo.version());
	}

	@Test
	void testExtractVersion() {
		var scraper = new PageScraper(URI.create("https://localhost/"), "<a>Exist 1.0</a>");
		assertEquals("1.0", scraper.extractVersion(Pattern.compile(">Exist ([.0-9]+)<")));
		assertEquals(null, scraper.extractVersion(Pattern.compile(">NotExist ([.0-9]+)<")));
	}

	@Test
	void testExtractGitHubPageFragmentLinks() {
		var baseUri = URI.create("https://github.com/google/guetzli/releases");
		var html = "<include-fragment loading=\"lazy\" src=\"https://github.com/google/guetzli/releases/expanded_assets/v1.0.1\" >"
				+ "<include-fragment loading=\"lazy\" src=\"https://github.com/google/guetzli/releases/expanded_assets/v1.0\" >";

		// Found
		var scraper = new PageScraper(baseUri, html);
		var actual = scraper.extractGitHubPageFragmentLinks();
		assertEquals(2, actual.size());
		assertEquals("https://github.com/google/guetzli/releases/expanded_assets/v1.0.1", actual.get(0).toString());
		assertEquals("https://github.com/google/guetzli/releases/expanded_assets/v1.0", actual.get(1).toString());

		// Not found
		scraper = new PageScraper(baseUri, StringUtils.EMPTY);
		assertTrue(scraper.extractGitHubPageFragmentLinks().isEmpty());
	}

}
