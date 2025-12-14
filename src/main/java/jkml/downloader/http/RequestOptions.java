package jkml.downloader.http;

import java.time.Instant;

public class RequestOptions {

	private UserAgent userAgent;

	private Referer referer;

	private Instant ifModifiedSince;

	public UserAgent getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(UserAgent userAgent) {
		this.userAgent = userAgent;
	}

	public Referer getReferer() {
		return referer;
	}

	public void setReferer(Referer referer) {
		this.referer = referer;
	}

	public Instant getIfModifiedSince() {
		return ifModifiedSince;
	}

	public void setIfModifiedSince(Instant ifModifiedSince) {
		this.ifModifiedSince = ifModifiedSince;
	}

	public static RequestOptions copy(RequestOptions original) {
		if (original == null) {
			return null;
		}
		var copy = new RequestOptions();
		copy.userAgent = original.userAgent;
		copy.referer = original.referer;
		copy.ifModifiedSince = original.ifModifiedSince;
		return copy;
	}

}
