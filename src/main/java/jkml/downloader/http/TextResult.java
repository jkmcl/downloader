package jkml.downloader.http;

public record TextResult(Status status, String text, String errorMessage) {

	public TextResult(String text) {
		this(Status.OK, text, null);
	}

}
