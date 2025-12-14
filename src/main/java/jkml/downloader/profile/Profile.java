package jkml.downloader.profile;

import java.net.URI;
import java.nio.file.Path;
import java.util.regex.Pattern;

import jkml.downloader.html.Occurrence;
import jkml.downloader.http.RequestOptions;

public class Profile {

	public enum Type {
		DIRECT, REDIRECT, STANDARD, GITHUB
	}

	private String name;

	private Type type;

	private URI fileUrl;

	private URI pageUrl;

	private Pattern linkPattern;

	private Occurrence linkOccurrence;

	private Pattern versionPattern;

	private RequestOptions requestOptions;

	private boolean skipIfFileExists;

	private Path outputDirectory;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public URI getFileUrl() {
		return fileUrl;
	}

	public void setFileUrl(URI fileUrl) {
		this.fileUrl = fileUrl;
	}

	public URI getPageUrl() {
		return pageUrl;
	}

	public void setPageUrl(URI pageUrl) {
		this.pageUrl = pageUrl;
	}

	public Pattern getLinkPattern() {
		return linkPattern;
	}

	public void setLinkPattern(Pattern linkPattern) {
		this.linkPattern = linkPattern;
	}

	public Occurrence getLinkOccurrence() {
		return linkOccurrence;
	}

	public void setLinkOccurrence(Occurrence linkOccurrence) {
		this.linkOccurrence = linkOccurrence;
	}

	public Pattern getVersionPattern() {
		return versionPattern;
	}

	public void setVersionPattern(Pattern versionPattern) {
		this.versionPattern = versionPattern;
	}

	public RequestOptions getRequestOptions() {
		return RequestOptions.copy(requestOptions);
	}

	public void setRequestOptions(RequestOptions requestOptions) {
		this.requestOptions = RequestOptions.copy(requestOptions);
	}

	public boolean isSkipIfFileExists() {
		return skipIfFileExists;
	}

	public void setSkipIfFileExists(boolean skipIfFileExists) {
		this.skipIfFileExists = skipIfFileExists;
	}

	public Path getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(Path outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

}
