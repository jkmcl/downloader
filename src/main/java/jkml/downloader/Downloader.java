package jkml.downloader;

import java.io.Closeable;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private final WebClient webClient;

	public Downloader() {
		this(new WebClient());
	}

	Downloader(WebClient webClient) {
		this.webClient = webClient;
	}

	@Override
	public void close() {
		webClient.close();
	}

	public void download(Path path) {
		if (Files.notExists(path)) {
			logger.error("File not found: {}", path);
			return;
		}

		var profileManager = new ProfileManager();

		List<Profile> profiles;
		try {
			profiles = profileManager.load(path);
		} catch (Exception e) {
			logErrroDuringOperation("profile loading", e.getMessage());
			return;
		}

		var errors = profileManager.validate(profiles);
		if (!errors.isEmpty()) {
			for (var error : errors) {
				logger.error(error);
			}
			return;
		}

		for (var profile : profiles) {
			download(profile);
			logger.info(StringUtils.EMPTY);
		}
	}

	void download(Profile profile) {
		URI fileLink;
		String fileName;

		logger.info("Looking for new version of {}", profile.getName());

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
			logger.error("Unsupported profile type: {}", type.name());
			return;
		}

		getFile(fileLink, profile.getRequestOptions(), profile.getOutputDirectory().resolve(fileName),
				profile.isSkipIfFileExists());
	}

	private void getFile(URI uri, RequestOptions options, Path path, boolean skipIfFileExists) {
		if (skipIfFileExists && Files.exists(path)) {
			logger.info("Local file exists");
			return;
		}
		try {
			var result = webClient.saveToFile(uri, options, path);
			if (result.status() == Status.OK) {
				logger.atInfo().log("Downloaded remote file last modified at {}",
						TimeUtils.formatter.format(result.lastModified()));
				logger.info("URL:  {}", uri);
				logger.info("Path: {}", path);
			} else {
				logger.info("Local file up to date");
			}
		} catch (Exception e) {
			logErrroDuringOperation("file download", e.getMessage());
		}
	}

	private String getText(URI uri, RequestOptions options) {
		try {
			return webClient.getContent(uri, options);
		} catch (Exception e) {
			logErrroDuringOperation("page retrieval", e.getMessage());
			return null;
		}
	}

	private URI getLink(URI uri, RequestOptions options) {
		try {
			return webClient.getLocation(uri, options);
		} catch (Exception e) {
			logErrroDuringOperation("location retrieval", e.getMessage());
			return null;
		}
	}

	private void logErrroDuringOperation(String operation, String errorMessage) {
		logger.error("Error occurred during {}: {}", operation, errorMessage);
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
		var fileInfo = pageScraper.extractFileInfo(profile.getLinkPattern(), profile.getLinkOccurrence(),
				profile.getVersionPattern());
		if (fileInfo == null) {
			if (isGitHub(pageLink) || profile.getType() == Type.GITHUB) {
				return findFileInfoInGitHubPageFragments(profile, pageScraper);
			}
			logger.error("File link not found in page");
		}

		return fileInfo;
	}

	private FileInfo findFileInfoInGitHubPageFragments(Profile profile, PageScraper pageScraper) {
		var fragmentLinks = pageScraper.extractGitHubPageFragmentLinks();
		if (fragmentLinks.isEmpty()) {
			logger.error("File link and page fragment link not found in page");
			return null;
		}

		for (var link : fragmentLinks) {
			var fragmentHtml = getText(link, profile.getRequestOptions());
			if (fragmentHtml == null) {
				return null;
			}

			// Use parent base URL for link resolution
			var fragmentScraper = new PageScraper(profile.getPageUrl(), fragmentHtml);
			var fileInfo = fragmentScraper.extractFileInfo(profile.getLinkPattern(), profile.getLinkOccurrence(),
					profile.getVersionPattern());
			if (fileInfo != null) {
				return fileInfo;
			}
		}

		logger.error("File link not found in any page fragment");
		return null;
	}

}
