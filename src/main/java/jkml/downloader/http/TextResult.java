package jkml.downloader.http;

public record TextResult(Status status, String text) {

	public TextResult(String text) {
		this(Status.OK, text);
	}

}
