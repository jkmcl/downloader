package jkml.downloader;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;

import jkml.downloader.html.FileInfo;
import jkml.downloader.html.PageScraper;
import jkml.downloader.http.RequestOptions;
import jkml.downloader.http.Status;
import jkml.downloader.http.WebClient;
import jkml.downloader.profile.Profile;
import jkml.downloader.profile.Profile.Type;
import jkml.downloader.profile.ProfileManager;
import jkml.downloader.util.FileUtils;
import jkml.downloader.util.StringUtils;
import jkml.downloader.util.TimeUtils;

public class Downloader implements Closeable {

	private final Logger logger = LoggerFactory.getLogger(Downloader.class);

	private final PrintStream printStream;

	private final WebClient webClient;

	public Downloader(PrintStream printStream) {
		this(printStream, new WebClient());
	}

	Downloader(PrintStream printStream, WebClient webClient) {
		this.printStream = printStream;
		this.webClient = webClient;
	}

	@Override
	public void close() throws IOException {
		webClient.close();
		printStream.close();
	}

	public void download(Path path) {
		var profileManager = new ProfileManager();
		if (profileManager.loadProfiles(path)) {
			for (var profile : profileManager.getProfiles()) {
				download(profile);
				printInfo(StringUtils.EMPTY);
			}
		} else {
			for (var error : profileManager.getErrors()) {
				printError(error);
			}
		}
	}

	void download(Profile profile) {
		URI fileUri;
		String fileName;

		printInfo("Looking for new version of {}", profile.getName());

		var type = profile.getType();
		if (type == Profile.Type.DIRECT || type == Profile.Type.REDIRECT) {
			fileUri = profile.getFileUrl();
			// Get actual file URL from location header in response
			if (type == Profile.Type.REDIRECT) {
				fileUri = getLink(fileUri, profile.getRequestOptions());
				if (fileUri == null) {
					return;
				}
				fileUri = profile.getFileUrl().resolve(fileUri);
			}
			fileName = FileUtils.getFileName(fileUri);
		} else if (type == Profile.Type.STANDARD || type == Profile.Type.GITHUB) {
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
		} else {
			printError("Unsupported profile type: {}", type.name());
			return;
		}

		downloadFile(fileUri, profile.getRequestOptions(), profile.getOutputDirectory().resolve(fileName));
	}

	private void downloadFile(URI uri, RequestOptions options, Path path) {
		var result = webClient.saveToFile(uri, options, path);

		switch (result.status()) {
		case NOT_MODIFIED:
			printInfo("Local file up to date");
			break;
		case OK:
			printInfo("Downloaded remote file last modified at {}", TimeUtils.Formatter.format(result.lastModified()));
			printInfo("URL:  {}", uri);
			printInfo("Path: {}", path);
			break;
		case ERROR:
			printErrorDuringOperation("file download", result.exception());
			break;
		}
	}

	private String getPage(URI uri, RequestOptions options) {
		var result = webClient.getContent(uri, options);
		if (result.status() != Status.OK) {
			printErrorDuringOperation("page retrieval", result.exception());
			return null;
		}
		return result.text();
	}

	private URI getLink(URI uri, RequestOptions options) {
		var result = webClient.getLocation(uri, options);
		if (result.status() != Status.OK) {
			printErrorDuringOperation("location retrieval", result.exception());
			return null;
		}
		return result.link();
	}

	private void printErrorDuringOperation(String operation, Throwable exception) {
		printError("Error occurred during {}: {}: {}", operation, exception.getClass().getName(), exception.getMessage());
	}

	private static boolean isGitHub(URI uri) {
		var host = uri.getHost().toLowerCase();
		return "github.com".equals(host) || host.endsWith(".github.com");
	}

	private FileInfo findFileInfo(Profile profile) {
		var pageUri = profile.getPageUrl();

		// Download page containing file info
		var pageHtml = getPage(pageUri, profile.getRequestOptions());
		if (pageHtml == null) {
			return null;
		}

		var pageScraper = new PageScraper(pageUri, pageHtml);
		var fileInfo = pageScraper.extractFileInfo(profile.getLinkPattern(), profile.getLinkOccurrence(), profile.getVersionPattern());
		if (fileInfo == null) {
			if (isGitHub(pageUri) || profile.getType() == Type.GITHUB) {
				return findFileInfoInGitHubPageFragments(profile, pageScraper);
			}
			printError("File link not found in page");
		}

		return fileInfo;
	}

	private FileInfo findFileInfoInGitHubPageFragments(Profile profile, PageScraper pageScraper) {
		var fragmentUriList = pageScraper.extractGitHubPageFragmentUriList();
		if (fragmentUriList.isEmpty()) {
			printError("File link and page fragment link not found in page");
			return null;
		}

		for (var fragmentUri : fragmentUriList) {
			var fragmentHtml = getPage(fragmentUri, profile.getRequestOptions());
			if (fragmentHtml == null) {
				return null;
			}

			// Use parent base URL for link resolution
			var fragmentScraper = new PageScraper(profile.getPageUrl(), fragmentHtml);
			var fileInfo = fragmentScraper.extractFileInfo(profile.getLinkPattern(), profile.getLinkOccurrence(), profile.getVersionPattern());
			if (fileInfo != null) {
				return fileInfo;
			}
		}

		printError("File link not found in any page fragment");
		return null;
	}

	private void printInfo(String format, Object... arguments) {
		printLine(Level.INFO, MessageFormatter.basicArrayFormat(format, arguments));
	}

	private void printError(String format, Object... arguments) {
		printLine(Level.ERROR, MessageFormatter.basicArrayFormat(format, arguments));
	}

	private void printLine(Level level, String message) {
		logger.atLevel(level).log(message);
		if (printStream != null) {
			printStream.print(message);
			printStream.println();
		}
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.out.println("Usage: " + Downloader.class.getName() + " <file>");
			return;
		}

		var path = Path.of(args[0]);

		if (Files.notExists(path)) {
			System.out.println("File not found: " + path);
			return;
		}

		try (var downloader = new Downloader(System.out)) {
			downloader.download(path);
		}
	}

}
