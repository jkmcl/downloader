package jkml.downloader.html;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		// Find link
		var matcher = linkPattern.matcher(html);
		MatchResult matchResult = null;
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

		var link = matchResult.group(1);
		logger.info("Link found: {}", link);

		// Resolve link
		var fileUri = resolve(link);

		// Find version
		String version = null;
		if (versionPattern != null) {
			version = extractVersion(versionPattern);
		} else if (matchResult.groupCount() >= 2) {
			version = matchResult.group(2);
			logger.info("Version found in link: {}", version);
		}
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

		if (version != null) {
			logger.info("Version found in page: {}", version);
		}

		return version;
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
