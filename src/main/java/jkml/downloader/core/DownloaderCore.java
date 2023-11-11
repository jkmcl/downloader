package jkml.downloader.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;

import jkml.downloader.html.FileInfo;
import jkml.downloader.html.PageExtractor;
import jkml.downloader.http.SaveResult;
import jkml.downloader.http.WebClient;
import jkml.downloader.profile.Profile;
import jkml.downloader.profile.ProfileManager;
import jkml.downloader.util.FileUtils;
import jkml.downloader.util.StringUtils;
import jkml.downloader.util.TimeUtils;

public class DownloaderCore {

	private final Logger logger = LoggerFactory.getLogger(DownloaderCore.class);

	private final WebClient webClient;

	private final PrintWriter writer;

	public DownloaderCore(WebClient webClient) {
		this.webClient = webClient;
		writer = (System.console() == null) ? null : System.console().writer();
	}

	public void download(Path jsonFile) throws IOException {
		var profileManager = new ProfileManager();
		for (var profile : profileManager.loadProfiles(jsonFile)) {
			download(profile);
			writeInfo(StringUtils.EMPTY);
		}
	}

	void download(Profile profile) {
		URI fileUri = null;
		String fileName;

		formatInfo("Looking for new version of {}", profile.getName());

		if (profile.getFileUrl() != null) {
			fileUri = profile.getFileUrl();
			fileName = FileUtils.getFileName(fileUri);
		} else {
			// Find file link from page
			var fileInfo = findFileInfo(profile);
			if (fileInfo == null) {
				return;
			}
			fileUri = fileInfo.uri();
			fileName = FileUtils.getFileName(fileUri);

			// Add version if it is not already part of the file name
			var version = fileInfo.version();
			if (!StringUtils.isNullOrBlank(version) && !fileName.contains(version)) {
				fileName = FileUtils.updateFileName(fileName, version);
			}
		}

		var result = webClient.saveToFile(fileUri, profile.getRequestOptions(), profile.getOutputDirectory().resolve(fileName));
		if (result == null) {
			return;
		}

		printResult(fileUri, result);
	}

	void printResult(URI fileUri, SaveResult result) {
		switch (result.status()) {
		case NOT_MODIFIED:
			writeInfo("Local file up to date");
			break;
		case OK:
			formatInfo("Downloaded remote file last modified at {}", TimeUtils.FORMATTER.format(result.lastModified()));
			formatInfo("File URL:   {}", fileUri);
			formatInfo("Local path: {}", result.filePath());
			break;
		case ERROR:
			formatError("Error: {}", result.errorMessage());
			break;
		}
	}

	private FileInfo findFileInfo(Profile profile) {
		var pageUri = profile.getPageUrl();

		// Download page containing file info
		var pageHtml = webClient.readString(pageUri, profile.getRequestOptions());
		if (pageHtml == null) {
			formatError("Failed to download page from {}", pageUri);
			return null;
		}

		var pageExtractor = new PageExtractor(pageUri, pageHtml);

		if (profile.getType() == Profile.Type.MOZILLA) {
			var fileInfo = pageExtractor.extractMozillaFileInfo(profile.getLinkPattern().toString());
			if (fileInfo == null) {
				writeError("File link cannot be derived from page");
			}
			return fileInfo;
		}

		var fileInfo = pageExtractor.extractFileInfo(profile.getLinkPattern(), profile.getLinkOccurrence(), profile.getVersionPattern());

		if (fileInfo == null) {
			if (profile.getType() == Profile.Type.GITHUB) {
				return findFileInfoInGitHubPageFragments(profile, pageExtractor);
			}
			formatError("File link not found in page from {}", pageUri);
		}

		return fileInfo;
	}

	private FileInfo findFileInfoInGitHubPageFragments(Profile profile, PageExtractor pageExtractor) {
		var fragmentUriList = pageExtractor.extractGitHubPageFragmentUriList();
		if (fragmentUriList.isEmpty()) {
			writeError("Page fragment link not found");
			return null;
		}

		for (var fragmentUri : fragmentUriList) {
			var fragmentHtml = webClient.readString(fragmentUri, profile.getRequestOptions());
			if (fragmentHtml == null) {
				formatError("Failed to download page fragment from {}", fragmentUri);
				return null;
			}

			var fragExtractor = new PageExtractor(fragmentUri, fragmentHtml);
			var fileInfo = fragExtractor.extractFileInfo(profile.getLinkPattern(), profile.getLinkOccurrence(), profile.getVersionPattern());
			if (fileInfo != null) {
				return fileInfo;
			}
		}

		writeError("File link not found in any page fragment");
		return null;
	}

	private void formatInfo(String format, Object... arguments) {
		writeLine(Level.INFO, MessageFormatter.basicArrayFormat(format, arguments));
	}

	private void formatError(String format, Object... arguments) {
		writeLine(Level.ERROR, MessageFormatter.basicArrayFormat(format, arguments));
	}

	private void writeInfo(String message) {
		writeLine(Level.INFO, message);
	}

	private void writeError(String message) {
		writeLine(Level.ERROR, message);
	}

	private void writeLine(Level level, String message) {
		logger.atLevel(level).log(message);
		if (writer != null) {
			writer.print(message);
			writer.println();
		}
	}

}