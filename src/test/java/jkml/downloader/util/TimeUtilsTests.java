package jkml.downloader.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class TimeUtilsTests {

	@Test
	void test() {
		assertNotNull(TimeUtils.formatter.format(Instant.now()));
	}

}
