package jkml.downloader.http;

import java.util.EnumMap;
import java.util.Map;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.message.BasicHeader;

import jkml.downloader.util.PropertiesHelper;

class Headers {

	public static final Header ACCEPT = new BasicHeader(HttpHeaders.ACCEPT, ContentType.WILDCARD.toString());

	public static final Header ACCEPT_LANGUAGE = new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9");

	private static final Map<UserAgent, Header> USER_AGENTS = new EnumMap<>(UserAgent.class);

	static {
		var propertiesHelper = PropertiesHelper.create("http.properties");
		USER_AGENTS.put(UserAgent.CHROME, new BasicHeader(HttpHeaders.USER_AGENT, propertiesHelper.getRequired("user-agent.chrome")));
		USER_AGENTS.put(UserAgent.CURL, new BasicHeader(HttpHeaders.USER_AGENT, propertiesHelper.getRequired("user-agent.curl")));
	}

	public static Header userAgent(UserAgent userAgent) {
		return USER_AGENTS.get(userAgent);
	}

	private Headers() {
	}

}
