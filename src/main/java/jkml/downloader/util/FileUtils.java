package jkml.downloader.util;

import java.net.URI;

public class FileUtils {

	private FileUtils() {
	}

	public static String updateFileName(String fileName, String version) {
		var index = fileName.lastIndexOf('.');
		var sb = new StringBuilder();
		if (index > 0) {
			sb.append(fileName, 0, index).append('-').append(version).append(fileName, index, fileName.length());
		} else {
			sb.append(fileName).append('-').append(version);
		}
		return sb.toString();
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

}
