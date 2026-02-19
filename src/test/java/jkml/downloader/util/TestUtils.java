package jkml.downloader.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class TestUtils {

	private TestUtils() {
	}

	public static Path resourcesDirectory() {
		return Path.of("src/test/resources");
	}

	public static Path outputDirectory() {
		return Path.of("target/test-output");
	}

	public static void deleteDirectories(Path dir) throws IOException {
		if (dir == null || Files.notExists(dir)) {
			return;
		}

		Files.walkFileTree(dir, new SimpleFileVisitor<>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (exc != null) {
					throw exc;
				}
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}

		});
	}

}
