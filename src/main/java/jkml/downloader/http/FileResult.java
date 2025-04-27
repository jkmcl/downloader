package jkml.downloader.http;

import java.time.Instant;

public record FileResult(Status status, Instant lastModified, String errorMessage) {

	public FileResult(Instant lastModified) {
		this(Status.OK, lastModified, null);
	}

	public FileResult() {
		this(Status.NOT_MODIFIED, null, null);
	}

}
