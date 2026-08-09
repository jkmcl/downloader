package jkml.downloader.util;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.function.Executable;
import org.slf4j.Logger;

public class TestUtils {

	private TestUtils() {
	}

	public static Path resourcesDirectory() {
		return Path.of("src/test/resources");
	}

	public static void createFile(Path path) throws IOException {
		if (Files.notExists(path)) {
			Files.createFile(path);
		}
	}

	public static <T extends Throwable> T assertAndLogThrows(Class<T> expectedType, Executable executable, Logger logger) {
		var throwable = assertThrows(expectedType, executable);
		logger.info("Exception message: {}", throwable.getMessage());
		return throwable;
	}

}
