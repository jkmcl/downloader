package jkml.downloader;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
	public void close() {
		webClient.close();
		printStream.close();
	}

	public void download(Path path) {
		if (Files.notExists(path)) {
			printError("File not found: {}", path);
			return;
		}

		var profileManager = new ProfileManager();

		List<Profile> profiles;
		try {
			profiles = profileManager.load(path);
		} catch (IOException e) {
			printErrorDuringOperation("profile loading", e.getMessage());
			return;
		}

		var errors = profileManager.validate(profiles);
		if (!errors.isEmpty()) {
			for (var error : errors) {
				printError(error);
			}
			return;
		}

		for (var profile : profiles) {
			download(profile);
			printInfo(StringUtils.EMPTY);
		}
	}

	void download(Profile profile) {
		URI fileLink;
		String fileName;

		printInfo("Looking for new version of {}", profile.getName());

		var type = profile.getType();
		if (type == Profile.Type.DIRECT || type == Profile.Type.REDIRECT) {
			fileLink = profile.getFileUrl();
			// Get actual file URL from location header in response
			if (type == Profile.Type.REDIRECT) {
				fileLink = getLink(fileLink, profile.getRequestOptions());
				if (fileLink == null) {
					return;
				}
				fileLink = profile.getFileUrl().resolve(fileLink);
			}
			fileName = FileUtils.getFileName(fileLink);
		} else if (type == Profile.Type.STANDARD || type == Profile.Type.GITHUB) {
			// Find file link from page
			var fileInfo = findFileInfo(profile);
			if (fileInfo == null) {
				return;
			}
			fileLink = fileInfo.uri();
			fileName = FileUtils.getFileName(fileLink);

			// Add version if it is not already part of the file name
			var version = fileInfo.version();
			if (!StringUtils.isNullOrBlank(version) && !fileName.contains(version)) {
				fileName = FileUtils.updateFileName(fileName, version);
			}
		} else {
			printError("Unsupported profile type: {}", type.name());
			return;
		}

		getFile(fileLink, profile.getRequestOptions(), profile.getOutputDirectory().resolve(fileName));
	}

	private void getFile(URI uri, RequestOptions options, Path path) {
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
			printErrorDuringOperation("file download", result.errorMessage());
			break;
		}
	}

	private String getText(URI uri, RequestOptions options) {
		var result = webClient.getContent(uri, options);
		if (result.status() != Status.OK) {
			printErrorDuringOperation("page retrieval", result.errorMessage());
			return null;
		}
		return result.text();
	}

	private URI getLink(URI uri, RequestOptions options) {
		var result = webClient.getLocation(uri, options);
		if (result.status() != Status.OK) {
			printErrorDuringOperation("location retrieval", result.errorMessage());
			return null;
		}
		return result.link();
	}

	private void printErrorDuringOperation(String operation, String errorMessage) {
		printError("Error occurred during {}: {}", operation, errorMessage);
	}

	private static boolean isGitHub(URI uri) {
		var host = uri.getHost().toLowerCase();
		return "github.com".equals(host) || host.endsWith(".github.com");
	}

	private FileInfo findFileInfo(Profile profile) {
		var pageLink = profile.getPageUrl();

		// Download page containing file info
		var pageHtml = getText(pageLink, profile.getRequestOptions());
		if (pageHtml == null) {
			return null;
		}

		var pageScraper = new PageScraper(pageLink, pageHtml);
		var fileInfo = pageScraper.extractFileInfo(profile.getLinkPattern(), profile.getLinkOccurrence(), profile.getVersionPattern());
		if (fileInfo == null) {
			if (isGitHub(pageLink) || profile.getType() == Type.GITHUB) {
				return findFileInfoInGitHubPageFragments(profile, pageScraper);
			}
			printError("File link not found in page");
		}

		return fileInfo;
	}

	private FileInfo findFileInfoInGitHubPageFragments(Profile profile, PageScraper pageScraper) {
		var fragmentLinks = pageScraper.extractGitHubPageFragmentLinks();
		if (fragmentLinks.isEmpty()) {
			printError("File link and page fragment link not found in page");
			return null;
		}

		for (var link : fragmentLinks) {
			var fragmentHtml = getText(link, profile.getRequestOptions());
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

	public static void main(String... args) {
		if (args.length != 1) {
			System.out.println("Usage: " + Downloader.class.getName() + " <file>");
			return;
		}

		try (var downloader = new Downloader(System.out)) {
			downloader.download(Path.of(args[0]));
		}
	}

}
