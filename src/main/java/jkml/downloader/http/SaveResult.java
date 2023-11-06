package jkml.downloader.http;

import java.nio.file.Path;
import java.time.Instant;

public record SaveResult(Status status, Path filePath, Instant lastModified, String errorMessage) {

	public enum Status {
		OK, NOT_MODIFIED, ERROR
	}

	public SaveResult(Status status, Path filePath) {
		this(status, filePath, null, null);
	}

	public SaveResult(Status status, Path filePath, Instant lastModified) {
		this(status, filePath, lastModified, null);
	}

	public SaveResult(Status status, String errorMessage) {
		this(status, null, null, errorMessage);
	}

}
