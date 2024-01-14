package jkml.downloader.html;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jkml.downloader.util.StringUtils;

public class PageScraper {

	private static final Pattern GITHUB_PAGE_FRAGMENT_URI_PATTERN = Pattern.compile("src=(\"?)(\\S+/expanded_assets/\\S+)(\\1)");

	private final Logger logger = LoggerFactory.getLogger(PageScraper.class);

	private final URI baseUri;

	private final String html;

	public PageScraper(URI baseUri, String html) {
		this.baseUri = baseUri;
		this.html = html;
	}

	public FileInfo extractFileInfo(Pattern linkPattern, Occurrence linkOccurrence, Pattern versionPattern) {
		MatchResult matchResult = null;

		// Find link
		var matcher = linkPattern.matcher(html);
		while (matcher.find()) {
			matchResult = matcher.toMatchResult();
			if (linkOccurrence != Occurrence.LAST) {
				break;
			}
		}
		if (matchResult == null || matchResult.groupCount() < 1) {
			logger.info("Link not found");
			return null;
		}

		// Extract link
		var link = matchResult.group(1);
		logger.info("Link found: {}", link);

		// Resolve link
		var fileUri = resolve(link);

		// Find version in page
		if (versionPattern != null) {
			return new FileInfo(fileUri, extractVersion(versionPattern));
		}

		// Find version in link
		if (matchResult.groupCount() < 2) {
			logger.info("Version not found in link");
			return new FileInfo(fileUri, null);
		}

		// Extract version
		var version = matchResult.group(2);
		logger.info("Version found in link: {}", version);
		return new FileInfo(fileUri, version);
	}

	URI resolve(String str) {
		var uri = URI.create(str);
		if (!uri.isAbsolute()) {
			uri = baseUri.resolve(uri);
			logger.info("Link resolved: {}", uri);
		}
		return uri;
	}

	String extractVersion(Pattern pattern) {
		String version = null;

		var matcher = pattern.matcher(html);
		if (matcher.find()) {
			version = matcher.group(1);
		}

		if (version == null) {
			logger.info("Version not found in page");
		} else {
			logger.info("Version found in page: {}", version);
		}

		return version;
	}

	public FileInfo extractMozillaFileInfo(String osLangProduct) {
		var matcher = Pattern.compile(baseUri.getPath() + "(\\d+(\\.\\d+)*)/").matcher(html);

		var versionList = new ArrayList<MozillaVersion>();

		while (matcher.find()) {
			var version = MozillaVersion.parse(matcher.group(1));
			if (version != null) {
				versionList.add(version);
			}
		}
		if (versionList.isEmpty()) {
			logger.info("Version not found in page");
			return null;
		}

		// Determine latest version and its link
		versionList.sort(MozillaVersion::compare);
		var version = versionList.get(versionList.size() - 1).toString();
		logger.info("Latest version found in page: {}", version);
		var uri = baseUri.resolve(String.join(StringUtils.EMPTY, version, "/", osLangProduct, "%20Setup%20", version, ".exe"));
		logger.info("Link derived from latest version: {}", uri);

		return new FileInfo(uri, version);
	}

	public List<URI> extractGitHubPageFragmentUriList() {
		var result = new ArrayList<URI>();

		var matcher = GITHUB_PAGE_FRAGMENT_URI_PATTERN.matcher(html);
		while (matcher.find()) {
			var link = matcher.group(2);
			logger.info("Page fragment link found: {}", link);
			result.add(resolve(link));
		}

		if (result.isEmpty()) {
			logger.info("Page fragment link not found");
		}

		return result;
	}

}
