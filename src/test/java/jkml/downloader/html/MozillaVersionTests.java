package jkml.downloader.html;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MozillaVersionTests {

	@Test
	void testParseAndGetParts() {
		assertIterableEquals(List.of(0, 0), MozillaVersion.parse("0.0").getParts());
		assertIterableEquals(List.of(0, 1), MozillaVersion.parse("0.1").getParts());
		assertIterableEquals(List.of(1, 0), MozillaVersion.parse("1.0").getParts());
		assertIterableEquals(List.of(1, 1), MozillaVersion.parse("1.1").getParts());
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "1", "0.1", "1.0" })
	void testParseAndToString(String value) {
		assertEquals(value, MozillaVersion.parse(value).toString());
	}

	@Test
	void testParse_null() {
		assertThrows(NullPointerException.class, () -> MozillaVersion.parse(null));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "1.", ".", ".1", "1.0b1", "-1" })
	void testParse_invalid(String value) {
		assertThrows(IllegalArgumentException.class, () -> MozillaVersion.parse(value));
	}

	@Test
	void testCompareTo_null() {
		var version = MozillaVersion.parse("76.0");
		assertThrows(NullPointerException.class, () -> MozillaVersion.compare(version, null));
		assertThrows(NullPointerException.class, () -> MozillaVersion.compare(null, version));
	}

	@Test
	void testCompareTo_equal() {
		var x = MozillaVersion.parse("1");
		var y = MozillaVersion.parse("1.0");
		var z = MozillaVersion.parse("1.0.0");

		assertEquals(0, MozillaVersion.compare(x, y));
		assertEquals(0, MozillaVersion.compare(y, z));
		assertEquals(0, MozillaVersion.compare(z, x));
	}

	@ParameterizedTest
	@CsvSource(value = { "75.0|75.0.1", "74.0.1|75.0", "74.0|75.0" }, delimiter = '|')
	void testCompareTo_notEqual(String olderVersionString, String newerVersionString) {
		var olderVersion = MozillaVersion.parse(olderVersionString);
		var newerVersion = MozillaVersion.parse(newerVersionString);

		assertTrue(MozillaVersion.compare(olderVersion, newerVersion) < 0);
		assertTrue(MozillaVersion.compare(newerVersion, olderVersion) > 0);
	}

}
