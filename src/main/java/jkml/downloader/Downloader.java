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
		for (var profile : loadProfiles(path)) {
			download(profile);
			logger.info(StringUtils.EMPTY);
		}
	}

	List<Profile> loadProfiles(Path path) {
		try {
			var profiles = new ProfileManager().load(path);
			if (validateProfiles(profiles)) {
				return profiles;
			}
		} catch (Exception e) {
			logException(logger, "profile loading", e);
		}
		return List.of();
	}

	private boolean validateProfiles(List<Profile> profiles) {
		var valid = true;
		for (var i = 0; i < profiles.size(); ++i) {
			var errors = ProfileManager.validate(profiles.get(i));
			if (!errors.isEmpty()) {
				valid = false;
				logger.atError().log("Invalid profile[{}]: {}", i, String.join("; ", errors));
			}
		}
		return valid;
	}

	void download(Profile profile) {
		logger.info("Looking for new version of {}", profile.getName());
		var downloadable = switch (profile.getType()) {
			case Profile.Type.STANDARD, Profile.Type.GITHUB -> findInPage(profile);
			case Profile.Type.REDIRECT -> findInHeader(profile.getFileUrl(), profile.getRequestOptions());
			case Profile.Type.DIRECT -> new Downloadable(profile.getFileUrl());
		};
		download(profile, downloadable);
	}

	void download(Profile profile, Downloadable downloadable) {
		if (downloadable == null || StringUtils.isNullOrBlank(downloadable.fileName())) {
			return;
		}

		var path = profile.getOutputDirectory().resolve(downloadable.fileName());
		if (profile.isSkipIfFileExists() && Files.exists(path)) {
			logger.info("Local file exists");
			return;
		}
		try {
			var result = webClient.saveToFile(downloadable.uri(), profile.getRequestOptions(), path);
			if (result.status() == Status.OK) {
				logger.atInfo().log("Downloaded remote file last modified at {}", TimeUtils.format(result.lastModified()));
				logger.info("URL:  {}", downloadable.uri());
				logger.info("Path: {}", path);
			} else {
				logger.info("Local file up to date");
			}
		} catch (Exception e) {
			logException(logger, "file download", e);
		}
	}

	Downloadable findInHeader(URI uri, RequestOptions requestOptions) {
		try {
			var location = webClient.getLocation(uri, requestOptions);
			if (location != null) {
				return new Downloadable(uri.resolve(location));
			}
		} catch (Exception e) {
			logException(logger, "location retrieval", e);
		}
		return null;
	}

	Downloadable findInPage(Profile profile) {
		var fileInfo = findFileInfo(profile);
		return (fileInfo != null)
				? new Downloadable(fileInfo.uri(),
						DownloadUtils.getFileName(fileInfo.uri(), fileInfo.version(), profile.getFileNameTemplate()))
				: null;
	}

	private String getContent(URI uri, RequestOptions requestOptions) {
		try {
			return webClient.getContent(uri, requestOptions);
		} catch (Exception e) {
			logException(logger, "page retrieval", e);
		}
		return null;
	}

	FileInfo findFileInfo(Profile profile) {
		var html = getContent(profile.getPageUrl(), profile.getRequestOptions());
		if (html == null) {
			return null;
		}

		var pageScraper = new PageScraper(profile.getPageUrl(), html);
		var fileInfo = extractFileInfo(profile, pageScraper);
		if (fileInfo != null) {
			return fileInfo;
		}

		if (DownloadUtils.isGitHub(profile.getPageUrl()) || profile.getType() == Type.GITHUB) {
			var fragmentLinks = pageScraper.extractGitHubPageFragmentLinks();
			if (fragmentLinks.isEmpty()) {
				logger.error("File link and page fragment link not found in page");
				return null;
			}
			return findFileInfoInGitHubPageFragments(profile, fragmentLinks);
		}

		logger.error("File link not found in page");
		return null;
	}

	private FileInfo findFileInfoInGitHubPageFragments(Profile profile, List<URI> fragmentLinks) {
		for (var link : fragmentLinks) {
			var html = getContent(link, profile.getRequestOptions());
			if (html == null) {
				return null;
			}

			var fileInfo = extractFileInfo(profile, html);
			if (fileInfo != null) {
				return fileInfo;
			}
		}

		logger.error("File link not found in any page fragment");
		return null;
	}

	static void logException(Logger logger, String operation, Exception exception) {
		logger.atError().log("Error occurred during {}: {}", operation, exception.toString());
	}

	private static FileInfo extractFileInfo(Profile profile, PageScraper pageScraper) {
		return pageScraper.extractFileInfo(profile.getLinkPattern(), profile.getLinkOccurrence(),
				profile.getVersionPattern());
	}

	private static FileInfo extractFileInfo(Profile profile, String html) {
		return extractFileInfo(profile, new PageScraper(profile.getPageUrl(), html));
	}

}
