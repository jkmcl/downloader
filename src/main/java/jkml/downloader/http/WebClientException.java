package jkml.downloader.http;

import java.io.Serial;

public class WebClientException extends Exception {

	@Serial
	private static final long serialVersionUID = 1L;

	public WebClientException(String message) {
		super(message);
	}

}
