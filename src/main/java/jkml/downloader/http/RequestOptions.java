package jkml.downloader.http;

public class RequestOptions {

	private final UserAgent userAgent;

	private final Referer referer;

	public RequestOptions() {
		// Default constructor required by Gson for deserialization
		this(null, null);
	}

	public RequestOptions(UserAgent userAgent, Referer referer) {
		this.userAgent = userAgent;
		this.referer = referer;
	}

	public UserAgent getUserAgent() {
		return userAgent;
	}

	public Referer getReferer() {
		return referer;
	}

}
