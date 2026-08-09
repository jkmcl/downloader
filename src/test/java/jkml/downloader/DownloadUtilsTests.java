package jkml.downloader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DownloadUtilsTests {

	private static final String FILE_NAME = "a.txt";

	private static final URI FILE_LINK = URI.create("https://localhost/").resolve(FILE_NAME);

	@ParameterizedTest
	@ValueSource(strings = { "https://a.github.com", "https://github.com" })
	void testIsGitHub_true(String uri) {
		assertTrue(DownloadUtils.isGitHub(URI.create(uri)));
	}

	@Test
	void testIsGitHub_false() {
		assertFalse(DownloadUtils.isGitHub(FILE_LINK));
	}

	@Test
	void testGetFileName() {
		assertEquals(FILE_NAME, DownloadUtils.getFileName(FILE_LINK, null));
		assertEquals(FILE_NAME, DownloadUtils.getFileName(FILE_LINK, ""));
		assertEquals("a-1.0.txt", DownloadUtils.getFileName(URI.create("a-1.0.txt"), "1.0"));
		assertEquals("a-1.0-2.0.txt", DownloadUtils.getFileName(URI.create("a-1.0.txt"), "2.0"));
	}

}
