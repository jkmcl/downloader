package jkml.downloader.http;

import java.time.Instant;

public record FileResult(Status status, Instant lastModified) {

	public FileResult(Instant lastModified) {
		this(Status.OK, lastModified);
	}

	public FileResult() {
		this(Status.NOT_MODIFIED, null);
	}

}
