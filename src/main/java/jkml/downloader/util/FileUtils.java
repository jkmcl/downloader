package jkml.downloader.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public class FileUtils {

	private FileUtils() {
	}

	public static String updateFileName(String fileName, String version) {
		var index = fileName.lastIndexOf('.');
		if (index <= 0) {
			return fileName + "-" + version;
		}
		return fileName.substring(0, index) + "-" + version + fileName.substring(index);
	}

	public static String getFileName(URI uri) {
		var path = uri.getPath();
		if (path == null) {
			path = uri.getSchemeSpecificPart();
		}

		if (path.isEmpty()) {
			return path;
		}

		if (path.length() == 1 && path.charAt(0) == '/') {
			return StringUtils.EMPTY;
		}

		var index = path.lastIndexOf('/');
		if (index == -1) {
			return path;
		}

		return path.substring(index + 1);
	}

	public static Path createDirectories(Path dir) {
		try {
			return Files.createDirectories(dir);
		} catch (IOException e) {
			throw new UncheckedIOException(e.getMessage(), e);
		}
	}

	public static Instant getLastModifiedTime(Path path) {
		try {
			return Files.getLastModifiedTime(path).toInstant();
		} catch (IOException e) {
			throw new UncheckedIOException(e.getMessage(), e);
		}
	}

}
