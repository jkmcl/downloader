package jkml.downloader.http;

import java.nio.file.Path;
import java.time.Instant;

public record FileResult(Status status, Path filePath, Instant lastModified, Throwable exception) {

	public FileResult(Path path, Instant lastModified) {
		this(Status.OK, path, lastModified, null);
	}

	public FileResult() {
		this(Status.NOT_MODIFIED, null, null, null);
	}

	public FileResult(Throwable exception) {
		this(Status.ERROR, null, null, exception);
	}

}
