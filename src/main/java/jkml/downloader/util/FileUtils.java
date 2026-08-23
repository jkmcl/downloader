package jkml.downloader.util;

import java.net.URI;

public class FileUtils {

	private FileUtils() {
	}

	public static String createFileName(String template, String version) {
		return template.replace("${version}", StringUtils.isNullOrBlank(version) ? StringUtils.EMPTY : version);
	}

	public static String updateFileName(String name, String version) {
		var sb = new StringBuilder();

		var index = name.lastIndexOf('.');
		if (index > 0) {
			sb.append(name, 0, index);
		} else {
			sb.append(name);
		}

		if (!StringUtils.isNullOrBlank(version)) {
			sb.append('-').append(version);
		}

		if (index > 0) {
			sb.append(name, index, name.length());
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
