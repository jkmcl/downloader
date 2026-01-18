package jkml.downloader.http;

public class WebClientException extends Exception {

	private static final long serialVersionUID = 1L;

	public WebClientException(String message) {
		super(message);
	}

	public WebClientException(String message, Throwable cause) {
		super(message, cause);
	}

}
