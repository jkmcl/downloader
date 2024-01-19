package jkml.downloader.http;

public class RequestOptions {

	public static final UserAgent DEFAULT_USER_AGENT = UserAgent.CHROME;

	public static final Referer DEFAULT_REFERER = Referer.NONE;

	private final UserAgent userAgent;

	private final Referer referer;

	public RequestOptions() {
		// Default constructor required by Gson for deserialization
		this(DEFAULT_USER_AGENT, DEFAULT_REFERER);
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
