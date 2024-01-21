package jkml.downloader.http;

import java.net.URI;

public record LinkResult(Status status, URI link, Throwable exception) {

	public LinkResult(URI link) {
		this(Status.OK, link, null);
	}

	public LinkResult(Throwable exception) {
		this(Status.ERROR, null, exception);
	}

}
