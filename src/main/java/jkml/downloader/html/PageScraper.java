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

	public URI getBaseUri() {
		return baseUri;
	}

	/**
	 * Extract URI and optionally version of file
	 *
	 * @param linkPattern Regular expression of the file URL. The URL is expected to
	 *                    be in capturing group 1. Version is extracted from the
	 *                    optional capturing group 2
	 */
	public FileInfo extractFileInfo(Pattern linkPattern, Occurrence linkOccurrence) {
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

		// Extract link
		var link = matchResult.group(1);
		logger.info("Link found: {}", link);

		// Resolve link
		var fileUri = resolve(link);

		if (matchResult.groupCount() < 2) {
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

	/**
	 * Extract link and optionally version of file
	 *
	 * @param linkPattern    Regular expression of the file URL. The URL is expected
	 *                       to be in capturing group 1.
	 * @param versionPattern The regular expression used to find the version string
	 *                       in the page. The actual version number must be in
	 *                       capturing group 1. Can be null
	 */
	public FileInfo extractFileInfo(Pattern linkPattern, Occurrence linkOccurrence, Pattern versionPattern) {
		var result = extractFileInfo(linkPattern, linkOccurrence);
		if (result == null || versionPattern == null) {
			return result;
		}

		return new FileInfo(result.uri(), extractVersion(versionPattern));
	}

	String extractVersion(Pattern pattern) {
		String version = null;

		var matcher = pattern.matcher(html);
		if (matcher.find()) {
			version = matcher.group(1);
		}

		if (version != null) {
			logger.info("Version found in page: {}", version);
		} else {
			logger.info("Version not found in page");
		}

		return version;
	}

	/**
	 * Extract link and version from Mozilla page
	 * @param html          The HTML page
	 * @param baseUri       The HTML page URL which will be used to resolve the file URL if the latter is relative
	 * @param osLangProduct Example: "win64/en-US/Firefox" or "win64/en-US/Thunderbird"
	 *
	 * @return
	 */
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

	/**
	 * Extract GitHub page fragment URLs
	 */
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
