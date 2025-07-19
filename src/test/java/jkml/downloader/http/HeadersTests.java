package jkml.downloader.http;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class HeadersTests {

	@Test
	void testUserAgent() {
		for (var value : UserAgent.values()) {
			assertNotNull(Headers.userAgent(value));
		}
	}

}
