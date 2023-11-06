package jkml.downloader.http;

public class HttpStatusException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public HttpStatusException(int code) {
		super("Unexpected HTTP response status code: " + code);
	}

}
