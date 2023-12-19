package jkml.downloader.http;

public record TextResult(Status status, String text, Throwable exception) {

	public TextResult(Status status, String text) {
		this(status, text, null);
	}

	public TextResult(Status status, Throwable exception) {
		this(status, null, exception);
	}

}
