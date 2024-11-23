package jkml.downloader.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class TestUtils {

	private TestUtils() {
	}

	public static Path getResoureAsPath(String name) {
		try {
			return Path.of(LangUtils.getClassLoader().getResource(name).toURI());
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	public static String readResourceAsString(String name) {
		try {
			return Files.readString(getResoureAsPath(name));
		} catch (IOException e) {
			throw new UncheckedIOException(e.getMessage(), e);
		}
	}

	public static Path outputDirectory() {
		return Path.of("target/test-outputs");
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
