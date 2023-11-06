package jkml.downloader.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class TimeUtilsTests {

	@Test
	void test() {
		assertDoesNotThrow(() -> TimeUtils.FORMATTER.format(Instant.now()));
	}

}
