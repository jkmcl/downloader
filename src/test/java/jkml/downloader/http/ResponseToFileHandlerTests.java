package jkml.downloader.http;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jkml.downloader.util.TestUtils;

class ResponseToFileHandlerTests {

	private static Path outDir = TestUtils.getOutputDirectory();

	private static Path source = outDir.resolve("source.txt");

	private static Path target = outDir.resolve("target.txt");

	@BeforeAll
	static void beforeAll() throws IOException {
		Files.createDirectories(outDir);
	}

	@AfterAll
	static void afterAll() throws IOException {
		Files.deleteIfExists(source);
		Files.deleteIfExists(target);
	}

	@Test
	void testCheckFileContent() throws IOException {
		// Target file does not exist
		Files.deleteIfExists(source);
		Files.deleteIfExists(target);
		assertDoesNotThrow(() -> ResponseToFileHandler.checkFileContent(source, target));

		// Same size, different content
		Files.writeString(source, "1234");
		Files.writeString(target, "1235");
		assertDoesNotThrow(() -> ResponseToFileHandler.checkFileContent(source, target));

		// Same size, same content
		Files.writeString(source, "1234");
		Files.writeString(target, "1234");
		var ioException = assertThrows(IOException.class, () -> ResponseToFileHandler.checkFileContent(source, target));
		assertTrue(ioException.getMessage().contains("identical"));

		// Different size, source bigger than target
		Files.writeString(source, "1234");
		Files.writeString(target, "12");
		assertDoesNotThrow(() -> ResponseToFileHandler.checkFileContent(source, target));

		// Different size, source half the size of target
		Files.writeString(source, "12");
		Files.writeString(target, "1234");
		assertDoesNotThrow(() -> ResponseToFileHandler.checkFileContent(source, target));

		// Different size, source smaller than half the size of target
		Files.writeString(source, "12");
		Files.writeString(target, "12345");
		ioException = assertThrows(IOException.class, () -> ResponseToFileHandler.checkFileContent(source, target));
		assertTrue(ioException.getMessage().contains("smaller"));
	}

}
