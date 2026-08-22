package jkml.downloader.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.junit.jupiter.api.Test;

class FileUtilsTests {

	@Test
	void testUpdateFileName() {
		assertEquals("testing.exe", FileUtils.updateFileName("testing.exe", null));
		assertEquals("testing.exe", FileUtils.updateFileName("testing.exe", ""));
		assertEquals("testing.exe", FileUtils.updateFileName("testing.exe", " "));
		assertEquals("testing-1.1.exe", FileUtils.updateFileName("testing.exe", "1.1"));
		assertEquals("testing-1.1", FileUtils.updateFileName("testing", "1.1"));
		assertEquals("testing-1.1.", FileUtils.updateFileName("testing.", "1.1"));
		assertEquals(".testing-1.1.", FileUtils.updateFileName(".testing.", "1.1"));
		assertEquals(".testing-1.1", FileUtils.updateFileName(".testing", "1.1"));
	}

	@Test
	void testCreateFileName() {
		var template = "file_v${version}.txt";
		assertEquals("file_v.txt", FileUtils.createFileName(template, null));
		assertEquals("file_v.txt", FileUtils.createFileName(template, ""));
		assertEquals("file_v.txt", FileUtils.createFileName(template, " "));
		assertEquals("file_v1.0.txt", FileUtils.createFileName(template, "1.0"));
	}

	@Test
	void testGetFileName() {
		assertEquals("", FileUtils.getFileName(URI.create("")));
		assertEquals("", FileUtils.getFileName(URI.create("/")));
		assertEquals("a", FileUtils.getFileName(URI.create("a")));
		assertEquals("a", FileUtils.getFileName(URI.create("/a")));
		assertEquals("", FileUtils.getFileName(URI.create("/a/")));
		assertEquals("b", FileUtils.getFileName(URI.create("/a/b")));
		assertEquals("b", FileUtils.getFileName(URI.create("a:b")));
	}

}
