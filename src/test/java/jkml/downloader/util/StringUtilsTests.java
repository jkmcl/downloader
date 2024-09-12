package jkml.downloader.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class StringUtilsTests {

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", " ", "  " })
	void testIsNullOrBlank_True(String str) {
		assertTrue(StringUtils.isNullOrBlank(str));
	}

	@ParameterizedTest
	@ValueSource(strings = { "a", " a ", "a a" })
	void testIsNullOrBlank_False(String str) {
		assertFalse(StringUtils.isNullOrBlank(str));
	}

}
