package jkml.downloader.http;

import java.nio.file.Path;
import java.time.Instant;

public record FileResult(Status status, Path filePath, Instant lastModified, Throwable exception) {

	public FileResult(Status status, Path filePath) {
		this(status, filePath, null, null);
	}

	public FileResult(Status status, Path filePath, Instant lastModified) {
		this(status, filePath, lastModified, null);
	}

	public FileResult(Status status, Throwable exception) {
		this(status, null, null, exception);
	}

}
