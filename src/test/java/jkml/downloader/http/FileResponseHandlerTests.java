package jkml.downloader.http;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.message.BasicHttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jkml.downloader.util.TestUtils;

class FileResponseHandlerTests {

	private static final Logger logger = LoggerFactory.getLogger(FileResponseHandlerTests.class);

	private static Path source;

	private static Path target;

	@TempDir
	static Path tempDir;

	@BeforeAll
	static void beforeAll() {
		source = tempDir.resolve("source.txt");
		target = tempDir.resolve("target.txt");
	}

	@Test
	void testCheckFileName() {
		var fileName = "file.zip";

		var response = new BasicHttpResponse(HttpStatus.SC_OK);
		assertDoesNotThrow(() -> FileResponseHandler.checkFileName(fileName, response));

		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"file.zip\"");
		assertDoesNotThrow(() -> FileResponseHandler.checkFileName(fileName, response));

		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"different.zip\"");
		TestUtils.assertAndLogThrows(ResponseException.class, () -> FileResponseHandler.checkFileName(fileName, response), logger);
	}

	@Test
	void testCheckFileContent() throws IOException {
		// Target file does not exist
		Files.deleteIfExists(source);
		Files.deleteIfExists(target);
		assertDoesNotThrow(() -> FileResponseHandler.checkFileContent(source, target));

		// Same size, different content
		Files.writeString(source, "1234");
		Files.writeString(target, "1235");
		assertDoesNotThrow(() -> FileResponseHandler.checkFileContent(source, target));

		// Different size, source bigger than target
		Files.writeString(source, "1234");
		Files.writeString(target, "12");
		assertDoesNotThrow(() -> FileResponseHandler.checkFileContent(source, target));

		// Different size, source half the size of target
		Files.writeString(source, "12");
		Files.writeString(target, "1234");
		assertDoesNotThrow(() -> FileResponseHandler.checkFileContent(source, target));

		// Different size, source smaller than half the size of target
		Files.writeString(source, "12");
		Files.writeString(target, "12345");
		var ioException = TestUtils.assertAndLogThrows(ResponseException.class, () -> FileResponseHandler.checkFileContent(source, target), logger);
		assertTrue(ioException.getMessage().contains("smaller"));
	}

}
