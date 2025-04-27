package jkml.downloader.http;

import java.net.URI;

public record LinkResult(Status status, URI link, String errorMessage) {

	public LinkResult(URI link) {
		this(Status.OK, link, null);
	}

}
