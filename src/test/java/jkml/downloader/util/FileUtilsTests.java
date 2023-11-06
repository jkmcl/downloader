package jkml.downloader.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.junit.jupiter.api.Test;

class FileUtilsTests {

	@Test
	void testUpdateFileName() {
		assertEquals("testing-1.1.exe", FileUtils.updateFileName("testing.exe", "1.1"));
		assertEquals("testing-1.1", FileUtils.updateFileName("testing", "1.1"));
		assertEquals("testing-1.1.", FileUtils.updateFileName("testing.", "1.1"));
		assertEquals(".testing-1.1.", FileUtils.updateFileName(".testing.", "1.1"));
		assertEquals(".testing-1.1", FileUtils.updateFileName(".testing", "1.1"));
	}

	@Test
	void testGetFileName() {
		assertEquals("", FileUtils.getFileName(URI.create("")));
		assertEquals("", FileUtils.getFileName(URI.create("/")));
		assertEquals("a", FileUtils.getFileName(URI.create("a")));
		assertEquals("a", FileUtils.getFileName(URI.create("/a")));
		assertEquals("", FileUtils.getFileName(URI.create("/a/")));
		assertEquals("b", FileUtils.getFileName(URI.create("/a/b")));

		assertEquals("a", FileUtils.getFileName(URI.create("custom:a")));
	}

}
