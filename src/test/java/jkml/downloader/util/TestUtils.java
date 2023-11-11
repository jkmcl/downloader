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
			throw new IllegalArgumentException(e);
		}
	}

	public static String readResourceAsString(String name) {
		try {
			return Files.readString(getResoureAsPath(name));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Path getOutputDirectory() {
		return getResoureAsPath(".").resolve("testOutput");
	}

}