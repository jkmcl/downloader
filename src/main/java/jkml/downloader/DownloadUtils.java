package jkml.downloader;

import java.net.URI;
import java.util.Locale;

import jkml.downloader.util.FileUtils;
import jkml.downloader.util.StringUtils;

class DownloadUtils {

	private DownloadUtils() {
	}

	static boolean isGitHub(URI uri) {
		var host = uri.getHost().toLowerCase(Locale.ROOT);
		return "github.com".equals(host) || host.endsWith(".github.com");
	}

	static String getFileName(URI uri, String version) {
		var fileName = FileUtils.getFileName(uri);
		if (StringUtils.isNullOrBlank(version) || fileName.contains(version)) {
			return fileName;
		}
		return FileUtils.updateFileName(fileName, version);
	}

}
