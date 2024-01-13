package jkml.downloader.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

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
		return getResoureAsPath(".").resolve("testOutput");
	}

}
