package jkml.downloader.http;

public class RequestOptions {

	private UserAgent userAgent = UserAgent.CHROME;

	private Referer referer = Referer.NONE;

	public RequestOptions() {
		// Default constructor required for JSON binding via setters
	}

	public RequestOptions(UserAgent userAgent, Referer referer) {
		this.userAgent = userAgent;
		this.referer = referer;
	}

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

}
