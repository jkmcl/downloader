package jkml.downloader.http;

import java.io.Serial;

class ResponseException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	ResponseException(String message) {
		super(message);
	}

}
