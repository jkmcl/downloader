package jkml.downloader.http;

import java.time.Instant;

public record FileResult(Status status, Instant lastModified, Throwable exception) {

	public FileResult(Instant lastModified) {
		this(Status.OK, lastModified, null);
	}

	public FileResult() {
		this(Status.NOT_MODIFIED, null, null);
	}

	public FileResult(Throwable exception) {
		this(Status.ERROR, null, exception);
	}

}
