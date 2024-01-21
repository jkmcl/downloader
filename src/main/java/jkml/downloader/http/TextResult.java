package jkml.downloader.http;

public record TextResult(Status status, String text, Throwable exception) {

	public TextResult(String text) {
		this(Status.OK, text, null);
	}

	public TextResult(Throwable exception) {
		this(Status.ERROR, null, exception);
	}

}
